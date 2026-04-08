import SwiftUI
import RealityKit
import SceneViewSwift

/// 3D tab -- search Sketchfab for 3D models and view them in a 3D viewer.
///
/// Replaces the old hardcoded model gallery with live search against the
/// Sketchfab public API. Users type a query, see thumbnail results, and
/// can open a model in the SceneView 3D viewer.
struct ExploreTab: View {
    @State private var searchText = ""
    @State private var selectedModel: SketchfabModel?
    @State private var hasLoadedFeatured = false

    private let service = SketchfabService.shared

    private let suggestedSearches = [
        "car", "dragon", "furniture", "robot", "sword",
        "spaceship", "tree", "cat", "guitar", "sneaker"
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if service.results.isEmpty && !service.isSearching && service.errorMessage == nil {
                    landingView
                } else {
                    resultsView
                }
            }
            .navigationTitle("3D Models")
            .searchable(text: $searchText, prompt: "Search Sketchfab models...")
            .onSubmit(of: .search) {
                Task { await service.search(searchText) }
            }
            .sheet(item: $selectedModel) { model in
                SketchfabViewerSheet(model: model)
            }
            .task {
                if !hasLoadedFeatured {
                    hasLoadedFeatured = true
                    await service.loadFeatured()
                }
            }
        }
    }

    // MARK: - Landing View

    private var landingView: some View {
        ScrollView {
            VStack(spacing: SceneViewTheme.spacingXL) {
                // Hero
                VStack(spacing: 12) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 48))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [.blue, .purple],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .accessibilityHidden(true)

                    Text("Search 3D Models")
                        .font(.title2.bold())

                    Text("Browse millions of free 3D models on Sketchfab")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 40)

                // Suggested searches
                VStack(alignment: .leading, spacing: 12) {
                    Text("Popular Searches")
                        .font(.headline)
                        .padding(.horizontal)

                    LazyVGrid(columns: [
                        GridItem(.adaptive(minimum: 100, maximum: 150), spacing: 8)
                    ], spacing: 8) {
                        ForEach(suggestedSearches, id: \.self) { suggestion in
                            Button {
                                searchText = suggestion
                                Task { await service.search(suggestion) }
                            } label: {
                                Text(suggestion.capitalized)
                                    .font(.subheadline.weight(.medium))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(Color(.secondarySystemBackground))
                                    .clipShape(RoundedRectangle(cornerRadius: SceneViewTheme.chipRadius))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal)
                }

                // Version badge
                Text("SceneView v3.6.1")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .padding(.top, 20)
            }
        }
    }

    // MARK: - Results View

    private var resultsView: some View {
        ScrollView {
            LazyVGrid(columns: [
                GridItem(.adaptive(minimum: 150, maximum: 200), spacing: 12)
            ], spacing: 12) {
                ForEach(service.results) { model in
                    SketchfabCard(model: model) {
                        selectedModel = model
                    }
                    .onAppear {
                        if model == service.results.last && service.hasMore {
                            Task { await service.loadMore() }
                        }
                    }
                }
            }
            .padding()

            if service.isSearching {
                ProgressView()
                    .padding()
            }

            if let error = service.errorMessage {
                VStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.title2)
                        .foregroundStyle(.orange)
                    Text(error)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
            }
        }
    }
}

// MARK: - Sketchfab Card

private struct SketchfabCard: View {
    let model: SketchfabModel
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                // Thumbnail
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(
                            LinearGradient(
                                colors: [Color(.systemGray5), Color(.systemGray6)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(height: 120)

                    if let url = model.thumbnailURL {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(height: 120)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            case .failure:
                                Image(systemName: "cube.fill")
                                    .font(.system(size: 36))
                                    .foregroundStyle(.secondary)
                            case .empty:
                                ProgressView()
                            @unknown default:
                                EmptyView()
                            }
                        }
                    } else {
                        Image(systemName: "cube.fill")
                            .font(.system(size: 36))
                            .foregroundStyle(.secondary)
                    }

                    // Animated badge
                    if model.isAnimated {
                        VStack {
                            HStack {
                                Spacer()
                                Text("Animated")
                                    .statusBadge(color: .orange)
                            }
                            .padding(6)
                            Spacer()
                        }
                    }
                }
                .frame(height: 120)
                .clipShape(RoundedRectangle(cornerRadius: 12))

                VStack(spacing: 2) {
                    Text(model.name)
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    HStack(spacing: 4) {
                        Text(model.authorName)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                        Spacer()
                        if model.likeCount > 0 {
                            HStack(spacing: 2) {
                                Image(systemName: "heart.fill")
                                    .font(.caption2)
                                    .foregroundStyle(.red.opacity(0.7))
                                Text("\(model.likeCount)")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .padding(.horizontal, 4)
            }
            .padding(8)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(model.name) by \(model.authorName)")
        .accessibilityAddTraits(.isButton)
    }
}

// MARK: - Sketchfab Viewer Sheet

/// Shows a Sketchfab model's info and opens the viewer URL.
struct SketchfabViewerSheet: View {
    let model: SketchfabModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Large thumbnail
                if let url = model.thumbnailURL {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(height: 280)
                                .clipped()
                        case .failure:
                            placeholderImage
                        case .empty:
                            ProgressView()
                                .frame(height: 280)
                        @unknown default:
                            EmptyView()
                        }
                    }
                } else {
                    placeholderImage
                }

                // Model info
                List {
                    Section("Model Info") {
                        LabeledContent("Name", value: model.name)
                        LabeledContent("Author", value: model.authorName)
                        if model.vertexCount > 0 {
                            LabeledContent("Vertices", value: formatNumber(model.vertexCount))
                        }
                        if model.faceCount > 0 {
                            LabeledContent("Faces", value: formatNumber(model.faceCount))
                        }
                        LabeledContent("Animated", value: model.isAnimated ? "Yes" : "No")
                        LabeledContent("Likes", value: "\(model.likeCount)")
                    }

                    Section {
                        if let viewerURL = model.viewerURL {
                            Link(destination: viewerURL) {
                                Label("View on Sketchfab", systemImage: "safari")
                            }
                        }
                    }
                }
            }
            .navigationTitle(model.name)
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var placeholderImage: some View {
        ZStack {
            LinearGradient(
                colors: [Color(.systemGray4), Color(.systemGray5)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Image(systemName: "cube.fill")
                .font(.system(size: 56))
                .foregroundStyle(.secondary)
        }
        .frame(height: 280)
    }

    private func formatNumber(_ n: Int) -> String {
        if n >= 1_000_000 {
            return String(format: "%.1fM", Double(n) / 1_000_000)
        } else if n >= 1_000 {
            return String(format: "%.1fK", Double(n) / 1_000)
        }
        return "\(n)"
    }
}
