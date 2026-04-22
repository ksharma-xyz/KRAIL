import SwiftUI
import FirebaseCore
import FirebaseFirestore
import FirebaseAuth
import KrailApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        return true
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    // Initialized explicitly in init() AFTER Koin is ready.
    // Do NOT use a default property initializer here — stored property defaults run
    // before the init() body in Swift, which would instantiate IOSDeepLinkHandler
    // before KoinAppKt.doInitKoin() runs (Koin not ready → crash).
    private let deepLinkHandler: IOSDeepLinkHandler

    init() {
        KoinAppKt.doInitKoin()
        deepLinkHandler = IOSDeepLinkHandler()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Universal Links (https://ksharma-xyz.github.io/trip?d=...)
                // Fires on both cold start and when the app is already running.
                // The Kotlin Splash guard in KrailNavHost handles the cold-start case:
                // if Splash is still active, the hot-event is skipped and SplashEntry
                // handles navigation via consumePending() instead.
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { userActivity in
                    guard let url = userActivity.webpageURL else { return }
                    deepLinkHandler.handle(urlString: url.absoluteString)
                }
                // Handles custom-scheme URLs and any link not routed via NSUserActivity.
                .onOpenURL { url in
                    deepLinkHandler.handle(urlString: url.absoluteString)
                }
        }
    }
}
