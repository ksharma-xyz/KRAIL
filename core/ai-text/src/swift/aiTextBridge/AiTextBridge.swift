import Foundation
#if canImport(FoundationModels)
import FoundationModels
#endif

@objcMembers public class AiTextBridge: NSObject {

    /// Reason strings are the shared vocabulary in `AiUnavailableReasons`, not Swift's
    /// interpolation of `UnavailableReason`. Interpolating the enum sent Kotlin
    /// "deviceNotEligible"/"appleIntelligenceNotEnabled"/"modelNotReady", which matched
    /// nothing the caller tested for, so a model that was merely still downloading was
    /// reported to riders as a sentence they had written wrong. Keep these in step with
    /// `AiUnavailableReasons` in commonMain.
    public func checkAvailability(completion: @escaping (Bool, String) -> Void) {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            switch SystemLanguageModel.default.availability {
            case .available:
                completion(true, "available")
            case .unavailable(let reason):
                switch reason {
                case .appleIntelligenceNotEnabled:
                    completion(false, "needs_device_setting")
                case .modelNotReady:
                    completion(false, "model_downloading")
                case .deviceNotEligible:
                    completion(false, "device_unsupported")
                @unknown default:
                    completion(false, "device_unsupported")
                }
            @unknown default:
                completion(false, "check_failed")
            }
        } else {
            completion(false, "device_unsupported")
        }
        #else
        completion(false, "device_unsupported")
        #endif
    }

    public func summarize(text: String, completion: @escaping (String?) -> Void) {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            Task {
                let instructions = """
                Summarize this public transport service alert in one short, plain \
                sentence for a rider. Do not add information that is not present in \
                the alert.
                """
                do {
                    let session = LanguageModelSession(instructions: instructions)
                    let response = try await session.respond(to: text)
                    completion(response.content)
                } catch {
                    completion(nil)
                }
            }
        } else {
            completion(nil)
        }
        #else
        completion(nil)
        #endif
    }

    /// Flattens the extraction to plain primitives before crossing the `@objc` boundary —
    /// Swift structs (including `@Generable` ones) aren't representable there. Completion
    /// shape: (originText, destinationText, hasTimeIntent, isArrival, timeText, modeHints).
    public func extractTripIntent(
        text: String,
        completion: @escaping (String?, String?, Bool, Bool, String?, [String]) -> Void
    ) {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            Task {
                // Kept deliberately in step with `buildExtractionPrompt` in
                // AndroidAiTextService. This side used to be a shortened paraphrase of it
                // with no examples, and the two drifted where it mattered: Android listed
                // "let's go to X" in the lone-place rule and carried three worked examples,
                // this one listed neither. "lets go to work" came back with work in BOTH
                // fields and the rider got a trip from their work stop to their work stop.
                // The deterministic guard in AiSearchInputViewModel is what actually stops
                // that reaching the row; this is the half that stops the model producing it.
                let instructions = """
                You extract structured trip-planning fields from a rider's message for a \
                public transport app.

                Rules:
                - originText / destinationText are short place names as the rider said \
                them (e.g. "home", "Central Station"), never a full sentence. Omitted if \
                not mentioned.
                - isArrival is true for "arrive by"/"need to be there by"/"by <time>", \
                false for "leave at"/"leaving around"/"departing at". Omit the whole time \
                field if the rider gave no time at all.
                - timeText is the time phrase verbatim (e.g. "9am", "6:30pm"), never \
                resolved, and includes any day word said with it ("tomorrow at 9am", \
                "friday 6pm") because the day is resolved later from this same string.
                - modeHints lists any transport mode words mentioned, verbatim, lowercase. \
                Empty if none mentioned.
                - If the rider names only ONE place, decide which field it belongs in from \
                the phrasing. "go home", "let's go to X", "lets go to X", "take me to X", \
                "heading to X" and "I need to get to X" all mean X is the DESTINATION and \
                originText is omitted. Only put a lone place in originText when the wording \
                is clearly about leaving, such as "from X" or "leaving X". A rider saying \
                where they are going is far more common than a rider saying only where they \
                are.
                - NEVER put the same place in both originText and destinationText. A rider \
                who names one place has named one end of the trip, not both.
                - Never invent a field the rider didn't actually say. Omit it instead.

                Examples:
                "let's go home" -> destinationText "home", originText omitted.
                "lets go to work" -> destinationText "work", originText omitted.
                "leaving home around 9" -> originText "home", destinationText omitted, \
                isArrival false, timeText "around 9".
                "town hall to bondi junction by 6pm friday" -> originText "town hall", \
                destinationText "bondi junction", isArrival true, timeText "6pm friday".
                """
                do {
                    let session = LanguageModelSession(instructions: instructions)
                    let response = try await session.respond(
                        to: text,
                        generating: TripIntentGenerable.self
                    )
                    let value = response.content
                    completion(
                        value.originText,
                        value.destinationText,
                        value.isArrival != nil,
                        value.isArrival ?? false,
                        value.timeText,
                        value.modeHints ?? []
                    )
                } catch {
                    completion(nil, nil, false, false, nil, [])
                }
            }
        } else {
            completion(nil, nil, false, false, nil, [])
        }
        #else
        completion(nil, nil, false, false, nil, [])
        #endif
    }
}

#if canImport(FoundationModels)
@available(iOS 26.0, *)
@Generable
struct TripIntentGenerable {
    @Guide(description: "Short place name as the rider said it, e.g. 'home' or 'Central Station'. Omit if not mentioned.")
    var originText: String?

    @Guide(description: "Same shape as originText, the rider's destination.")
    var destinationText: String?

    @Guide(description: "True for 'arrive by'/'need to be there by', false for 'leave at'/'leaving around'. Omit if the rider gave no time at all.")
    var isArrival: Bool?

    @Guide(description: "The time phrase verbatim, e.g. '9am', '6:30pm'. Omit if no time mentioned.")
    var timeText: String?

    @Guide(description: "Transport mode words the rider mentioned, verbatim, lowercase, e.g. 'train', 'bus'. Empty if none mentioned.")
    var modeHints: [String]?
}
#endif
