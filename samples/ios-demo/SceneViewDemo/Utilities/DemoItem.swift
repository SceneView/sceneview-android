import SwiftUI

/// Represents a single scene entry in the Scenes tab.
struct DemoItem: Identifiable {
    let id = UUID()
    let title: String
    let icon: String
    let subtitle: String
    let category: DemoCategory
    let destination: AnyView

    init<V: View>(
        title: String,
        icon: String,
        subtitle: String,
        category: DemoCategory,
        @ViewBuilder destination: () -> V
    ) {
        self.title = title
        self.icon = icon
        self.subtitle = subtitle
        self.category = category
        self.destination = AnyView(destination())
    }
}

/// Scene categories for grouping.
enum DemoCategory: String, CaseIterable, Comparable {
    case geometry = "Geometry"
    case content = "Content"
    case lighting = "Lighting"
    case effects = "Effects"
    case advanced = "Advanced"
    case ar = "Augmented Reality"

    static func < (lhs: DemoCategory, rhs: DemoCategory) -> Bool {
        let order: [DemoCategory] = [.geometry, .content, .lighting, .effects, .advanced, .ar]
        let lhsIndex = order.firstIndex(of: lhs) ?? 0
        let rhsIndex = order.firstIndex(of: rhs) ?? 0
        return lhsIndex < rhsIndex
    }
}
