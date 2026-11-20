import SwiftUI
import UserNotifications
import Shared

/// Shows phase-end alerts as banners even while the app is open (iOS hides them by default).
final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}

@main
struct iOSApp: App {
    // The notification center holds its delegate weakly, so keep a strong reference here.
    private let notificationDelegate = NotificationDelegate()

    init() {
        // Start dependency injection once, before any shared UI is created.
        MainViewControllerKt.startKoinIos()
        UNUserNotificationCenter.current().delegate = notificationDelegate
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
