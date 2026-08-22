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
                let instructions = """
                Extract trip-planning fields from a rider's message for a public transport \
                app. originText/destinationText are short place names as the rider said \
                them (e.g. "home", "Central Station"), omitted if not mentioned. isArrival \
                is true for "arrive by"/"need to be there by", false for "leave at"/ \
                "leaving around" - omit the whole time field if the rider gave no time. \
                timeText is the time phrase verbatim (e.g. "9am", "6:30pm"), never \
                resolved, and includes any day word said with it ("tomorrow at 9am", \
                "friday 6pm") because the day is resolved later from this same string. \
                modeHints lists any transport mode words mentioned, verbatim, \
                lowercase. Never invent a field the rider didn't actually say. If the \
                rider names only one place, decide from the phrasing: "go home", "take \
                me to X" and "heading to X" mean X is the destination and origin is \
                omitted; only "from X" or "leaving X" makes a lone place the origin.
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
