import AVFoundation
import Foundation
import Speech

/// Wraps iOS 26's `SpeechAnalyzer` behind plain Objective-C entry points, because
/// Kotlin/Native cinterop reads Objective-C headers and `SpeechAnalyzer` is a Swift `actor`
/// with `AsyncSequence` results and no Objective-C surface at all.
///
/// Same shape and same reason as `AiTextBridge` in `:core:ai-text`: primitives and closures
/// only across the boundary, no Swift types, no generics, no async.
///
/// **This file owns mechanism, never policy.** It reports what Apple's modules say, including
/// `SpeechDetector`'s voice activity in audio time, and it does not decide when the rider has
/// finished talking. That decision lives in `SpeechActivityWatch` in `commonMain`, where a test
/// can reach it. Swift source in this project has no test task, which is exactly how three
/// rider-facing faults lived in the old `SFSpeechRecognizer` path unnoticed.
@objcMembers public class SpeechBridge: NSObject {

    private var session: AnyObject?

    /// Availability reasons are the shared vocabulary in `SpeechUnavailableReasons`, never
    /// Swift's own spelling of anything. The lesson `AiTextBridge` documents: a reason string
    /// only one side knows is a message the rider never sees.
    public func checkAvailability(completion: @escaping (Bool, String) -> Void) {
        guard #available(iOS 26.0, *) else {
            completion(false, "not_available")
            return
        }
        Task {
            let locale = Locale.current
            let installed = await DictationTranscriber.installedLocales
            if installed.contains(where: { $0.identifier(.bcp47) == locale.identifier(.bcp47) }) {
                completion(true, "available")
                return
            }
            let supported = await DictationTranscriber.supportedLocales
            if supported.contains(where: { $0.identifier(.bcp47) == locale.identifier(.bcp47) }) {
                // Downloadable, not missing. Same distinction the AI side draws, and the
                // caller words the two differently.
                completion(false, "model_downloading")
            } else {
                completion(false, "not_available")
            }
        }
    }

    /// Starts listening. Every callback may arrive on any thread.
    ///
    /// `onSpeechActivity` carries `SpeechDetector`'s answer and the audio time it applies to,
    /// in seconds. Audio time, not wall clock, is the whole reason this exists: it is when the
    /// rider spoke, not when a transcription happened to reach us.
    public func start(
        onPartial: @escaping (String) -> Void,
        onFinal: @escaping (String) -> Void,
        onSpeechActivity: @escaping (Bool, Double) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard #available(iOS 26.0, *) else {
            onError("not_available")
            return
        }
        let session = AnalyzerSession(
            onPartial: onPartial,
            onFinal: onFinal,
            onSpeechActivity: onSpeechActivity,
            onError: onError
        )
        self.session = session
        session.start()
    }

    /// Stops the microphone and asks for a final transcript. The caller still gets `onFinal`.
    public func finish() {
        guard #available(iOS 26.0, *), let session = session as? AnalyzerSession else { return }
        session.finish()
    }

    /// Tears everything down without waiting for a final transcript.
    public func cancel() {
        guard #available(iOS 26.0, *), let session = session as? AnalyzerSession else { return }
        session.cancel()
        self.session = nil
    }
}

@available(iOS 26.0, *)
private final class AnalyzerSession {

    private let onPartial: (String) -> Void
    private let onFinal: (String) -> Void
    private let onSpeechActivity: (Bool, Double) -> Void
    private let onError: (String) -> Void

    private let audioEngine = AVAudioEngine()
    private var analyzer: SpeechAnalyzer?
    private var transcriber: DictationTranscriber?
    private var inputBuilder: AsyncStream<AnalyzerInput>.Continuation?
    private var converter: AVAudioConverter?
    private var tasks: [Task<Void, Never>] = []
    private var didFinish = false

    init(
        onPartial: @escaping (String) -> Void,
        onFinal: @escaping (String) -> Void,
        onSpeechActivity: @escaping (Bool, Double) -> Void,
        onError: @escaping (String) -> Void
    ) {
        self.onPartial = onPartial
        self.onFinal = onFinal
        self.onSpeechActivity = onSpeechActivity
        self.onError = onError
    }

    func start() {
        Task { [weak self] in
            guard let self else { return }
            do {
                try await self.startAnalyzing()
            } catch {
                self.onError(error.localizedDescription)
            }
        }
    }

    private func startAnalyzing() async throws {
        // .progressiveShortDictation, not .shortDictation: the rider watches their words
        // arrive as they speak, so results have to be delivered live rather than at the end.
        // Not .phrase either, which produces no live results at all.
        let transcriber = DictationTranscriber(
            locale: Locale.current,
            preset: .progressiveShortDictation
        )
        self.transcriber = transcriber

        // Apple's own voice activity detection, which is the thing the old implementation had
        // to fake by watching the transcript change. It cannot run alone; pairing it with the
        // transcriber in one analyzer is the documented arrangement.
        let detector = SpeechDetector(
            detectionOptions: SpeechDetector.DetectionOptions(sensitivityLevel: .medium),
            reportResults: true
        )

        let analyzer = SpeechAnalyzer(modules: [detector, transcriber])
        self.analyzer = analyzer

        let (inputSequence, inputBuilder) = AsyncStream.makeStream(of: AnalyzerInput.self)
        self.inputBuilder = inputBuilder

        let analyzerFormat = await SpeechAnalyzer.bestAvailableAudioFormat(
            compatibleWith: [detector, transcriber]
        )

        try startMicrophone(analyzerFormat: analyzerFormat)
        try await analyzer.start(inputSequence: inputSequence)

        tasks.append(Task { [weak self] in
            guard let self else { return }
            do {
                for try await result in transcriber.results {
                    let text = String(result.text.characters)
                    if result.isFinal {
                        self.onFinal(text)
                    } else {
                        self.onPartial(text)
                    }
                }
            } catch {
                self.onError(error.localizedDescription)
            }
        })

        tasks.append(Task { [weak self] in
            guard let self else { return }
            do {
                for try await result in detector.results {
                    self.onSpeechActivity(
                        result.speechDetected,
                        CMTimeGetSeconds(result.range.end)
                    )
                }
            } catch {
                // A detector that stops reporting is not a failed session: the transcriber is
                // still running and the caller's own ceiling still ends it. Reporting an error
                // here would tear down a working transcription.
            }
        })
    }

    /// Feeds the microphone into the analyzer, converting to the format the modules asked for.
    ///
    /// The tap's own format is whatever the hardware gives; the analyzer publishes what it
    /// wants. Handing it the wrong one is silent, in that the session runs and never
    /// transcribes anything, so the conversion is not optional.
    private func startMicrophone(analyzerFormat: AVAudioFormat?) throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.record, mode: .measurement)
        try session.setActive(true, options: .notifyOthersOnDeactivation)

        let inputNode = audioEngine.inputNode
        let inputFormat = inputNode.outputFormat(forBus: 0)
        if let analyzerFormat, analyzerFormat != inputFormat {
            converter = AVAudioConverter(from: inputFormat, to: analyzerFormat)
        }

        inputNode.installTap(onBus: 0, bufferSize: 4096, format: inputFormat) { [weak self] buffer, _ in
            guard let self else { return }
            guard let converted = self.convert(buffer: buffer, to: analyzerFormat) else { return }
            self.inputBuilder?.yield(AnalyzerInput(buffer: converted))
        }

        audioEngine.prepare()
        try audioEngine.start()
    }

    private func convert(buffer: AVAudioPCMBuffer, to format: AVAudioFormat?) -> AVAudioPCMBuffer? {
        guard let format, let converter else { return buffer }
        let ratio = format.sampleRate / buffer.format.sampleRate
        let capacity = AVAudioFrameCount(Double(buffer.frameLength) * ratio) + 1024
        guard let output = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: capacity) else {
            return nil
        }
        var consumed = false
        var error: NSError?
        converter.convert(to: output, error: &error) { _, status in
            if consumed {
                status.pointee = .noDataNow
                return nil
            }
            consumed = true
            status.pointee = .haveData
            return buffer
        }
        return error == nil ? output : nil
    }

    /// The graceful finish. Apple is explicit that ending the input sequence does not end the
    /// session, so both are needed, and the microphone goes first: nothing should be arriving
    /// after we have said that was all the audio there is.
    func finish() {
        guard !didFinish else { return }
        didFinish = true
        stopMicrophone()
        inputBuilder?.finish()
        Task { [weak self] in
            guard let self, let analyzer = self.analyzer else { return }
            do {
                try await analyzer.finalizeAndFinishThroughEndOfInput()
            } catch {
                self.onError(error.localizedDescription)
            }
        }
    }

    func cancel() {
        didFinish = true
        stopMicrophone()
        inputBuilder?.finish()
        tasks.forEach { $0.cancel() }
        tasks.removeAll()
        let analyzer = self.analyzer
        Task { await analyzer?.cancelAndFinishNow() }
        self.analyzer = nil
    }

    private func stopMicrophone() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        try? AVAudioSession.sharedInstance().setActive(
            false,
            options: .notifyOthersOnDeactivation
        )
    }
}
