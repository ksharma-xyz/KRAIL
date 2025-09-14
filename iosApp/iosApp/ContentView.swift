import UIKit
import SwiftUI
import KrailApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onOpenURL { url in
                // Handle deep link through the KMP DeepLinkManager from core module
                DeepLinkBridgeKt.handleDeepLinkFromIos(url: url.absoluteString)
            }
    }
}
