import Foundation

/// Sketchfab API client for searching and downloading 3D models.
///
/// Uses the public Sketchfab API v3 -- no API key required for search.
/// Download requires authentication but search + thumbnails are free.
@MainActor
@Observable
final class SketchfabService {
    static let shared = SketchfabService()

    private(set) var results: [SketchfabModel] = []
    private(set) var isSearching = false
    private(set) var errorMessage: String?
    private(set) var hasMore = false

    private var currentQuery = ""
    private var nextCursor: String?
    private let session = URLSession.shared
    private let baseURL = "https://api.sketchfab.com/v3"

    private init() {}

    // MARK: - Search

    /// Searches Sketchfab for downloadable 3D models.
    ///
    /// - Parameter query: Search term (e.g., "car", "dragon", "furniture").
    func search(_ query: String) async {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            results = []
            hasMore = false
            return
        }

        currentQuery = trimmed
        nextCursor = nil
        isSearching = true
        errorMessage = nil

        do {
            let response = try await fetchPage(query: trimmed, cursor: nil)
            results = response.results.map(SketchfabModel.init)
            nextCursor = response.cursors?.next
            hasMore = nextCursor != nil
        } catch {
            errorMessage = error.localizedDescription
            results = []
        }

        isSearching = false
    }

    /// Loads the next page of results for the current query.
    func loadMore() async {
        guard let cursor = nextCursor, !isSearching else { return }

        isSearching = true
        do {
            let response = try await fetchPage(query: currentQuery, cursor: cursor)
            let newModels = response.results.map(SketchfabModel.init)
            results.append(contentsOf: newModels)
            nextCursor = response.cursors?.next
            hasMore = nextCursor != nil
        } catch {
            errorMessage = error.localizedDescription
        }
        isSearching = false
    }

    /// Fetches featured/popular models as a default landing page.
    func loadFeatured() async {
        isSearching = true
        errorMessage = nil

        do {
            let response = try await fetchPage(query: "", cursor: nil, sortBy: "-likeCount")
            results = response.results.map(SketchfabModel.init)
            nextCursor = response.cursors?.next
            hasMore = nextCursor != nil
        } catch {
            errorMessage = error.localizedDescription
        }
        isSearching = false
    }

    // MARK: - Networking

    private func fetchPage(
        query: String,
        cursor: String?,
        sortBy: String = "-relevance"
    ) async throws -> SketchfabSearchResponse {
        var components = URLComponents(string: "\(baseURL)/search")!
        var queryItems: [URLQueryItem] = [
            URLQueryItem(name: "type", value: "models"),
            URLQueryItem(name: "downloadable", value: "true"),
            URLQueryItem(name: "count", value: "24"),
            URLQueryItem(name: "sort_by", value: sortBy),
        ]

        if !query.isEmpty {
            queryItems.append(URLQueryItem(name: "q", value: query))
        }

        if let cursor {
            queryItems.append(URLQueryItem(name: "cursor", value: cursor))
        }

        components.queryItems = queryItems

        guard let url = components.url else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 15

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if httpResponse.statusCode == 429 {
            throw SketchfabError.rateLimited
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw SketchfabError.httpError(httpResponse.statusCode)
        }

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return try decoder.decode(SketchfabSearchResponse.self, from: data)
    }
}

// MARK: - Models

/// A Sketchfab 3D model search result.
struct SketchfabModel: Identifiable, Hashable {
    let id: String
    let name: String
    let authorName: String
    let thumbnailURL: URL?
    let viewerURL: URL?
    let vertexCount: Int
    let faceCount: Int
    let isAnimated: Bool
    let likeCount: Int

    init(from dto: SketchfabModelDTO) {
        self.id = dto.uid
        self.name = dto.name
        self.authorName = dto.user?.displayName ?? "Unknown"
        self.likeCount = dto.likeCount ?? 0
        self.vertexCount = dto.vertexCount ?? 0
        self.faceCount = dto.faceCount ?? 0
        self.isAnimated = dto.isAnimated ?? false

        // Pick the best thumbnail: 200x200 > 100x100 > any
        if let images = dto.thumbnails?.images {
            let sorted = images.sorted { ($0.width ?? 0) > ($1.width ?? 0) }
            let preferred = sorted.first { ($0.width ?? 0) >= 200 } ?? sorted.first
            self.thumbnailURL = preferred?.url.flatMap(URL.init(string:))
        } else {
            self.thumbnailURL = nil
        }

        self.viewerURL = URL(string: "https://sketchfab.com/3d-models/\(dto.uid)")
    }

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: SketchfabModel, rhs: SketchfabModel) -> Bool { lhs.id == rhs.id }
}

// MARK: - DTOs

struct SketchfabSearchResponse: Decodable {
    let results: [SketchfabModelDTO]
    let cursors: Cursors?

    struct Cursors: Decodable {
        let next: String?
        let previous: String?
    }
}

struct SketchfabModelDTO: Decodable {
    let uid: String
    let name: String
    let user: UserDTO?
    let thumbnails: ThumbnailsDTO?
    let vertexCount: Int?
    let faceCount: Int?
    let isAnimated: Bool?
    let likeCount: Int?

    struct UserDTO: Decodable {
        let displayName: String?
    }

    struct ThumbnailsDTO: Decodable {
        let images: [ImageDTO]?
    }

    struct ImageDTO: Decodable {
        let url: String?
        let width: Int?
        let height: Int?
    }
}

// MARK: - Errors

enum SketchfabError: LocalizedError {
    case rateLimited
    case httpError(Int)

    var errorDescription: String? {
        switch self {
        case .rateLimited:
            return "Rate limited by Sketchfab. Please wait a moment and try again."
        case .httpError(let code):
            return "Sketchfab returned HTTP \(code)."
        }
    }
}
