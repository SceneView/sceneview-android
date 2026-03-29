#if os(iOS)
import UIKit

/// Centralized haptic feedback for the demo app.
@MainActor
enum HapticManager {
    /// Light tap -- used for button presses and selections.
    static func lightTap() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()
    }

    /// Medium tap -- used for placing objects in AR.
    static func mediumTap() {
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
    }

    /// Heavy tap -- used for significant actions.
    static func heavyTap() {
        let generator = UIImpactFeedbackGenerator(style: .heavy)
        generator.impactOccurred()
    }

    /// Selection changed -- used for picker and segment changes.
    static func selectionChanged() {
        let generator = UISelectionFeedbackGenerator()
        generator.selectionChanged()
    }

    /// Success notification.
    static func success() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }

    /// Error notification.
    static func error() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.error)
    }
}
#endif
