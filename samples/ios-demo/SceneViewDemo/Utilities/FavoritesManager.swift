import SwiftUI

/// Manages user favorites using UserDefaults.
@MainActor
@Observable
final class FavoritesManager {
    static let shared = FavoritesManager()

    private let key = "favoriteModels"
    private(set) var favorites: Set<String>

    private init() {
        let saved = UserDefaults.standard.stringArray(forKey: key) ?? []
        self.favorites = Set(saved)
    }

    func isFavorite(_ modelName: String) -> Bool {
        favorites.contains(modelName)
    }

    func toggle(_ modelName: String) {
        if favorites.contains(modelName) {
            favorites.remove(modelName)
        } else {
            favorites.insert(modelName)
        }
        UserDefaults.standard.set(Array(favorites), forKey: key)
    }
}
