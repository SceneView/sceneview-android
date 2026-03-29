#if os(iOS)
import RealityKit
import UIKit

/// Screenshot/snapshot API for capturing the scene as an image.
///
/// Provides utility methods to capture the current RealityKit scene content
/// as a `UIImage` for sharing, thumbnails, or saving.
///
/// ```swift
/// // Capture a snapshot from an ARView
/// if let image = SceneSnapshot.capture(from: arView) {
///     UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
/// }
/// ```
public enum SceneSnapshot {

    /// Captures a snapshot of an ARView as a UIImage.
    ///
    /// Uses ARView's built-in snapshot capability.
    ///
    /// - Parameters:
    ///   - arView: The ARView to capture.
    ///   - completion: Closure called with the captured image, or nil on failure.
    public static func capture(
        from arView: ARView,
        completion: @escaping (UIImage?) -> Void
    ) {
        arView.snapshot(saveToHDR: false) { image in
            completion(image)
        }
    }

    /// Captures a snapshot of an ARView as a UIImage using async/await.
    ///
    /// - Parameter arView: The ARView to capture.
    /// - Returns: The captured UIImage, or nil on failure.
    @MainActor
    public static func capture(from arView: ARView) async -> UIImage? {
        await withCheckedContinuation { continuation in
            arView.snapshot(saveToHDR: false) { image in
                continuation.resume(returning: image)
            }
        }
    }

    /// Captures a snapshot of a UIView (for RealityView or any SwiftUI-hosted view).
    ///
    /// - Parameters:
    ///   - view: The UIView to capture.
    ///   - scale: Render scale. Default is the main screen scale.
    /// - Returns: The rendered UIImage.
    public static func captureView(
        _ view: UIView,
        scale: CGFloat = UITraitCollection.current.displayScale
    ) -> UIImage? {
        let renderer = UIGraphicsImageRenderer(
            size: view.bounds.size,
            format: {
                let format = UIGraphicsImageRendererFormat()
                format.scale = scale
                return format
            }()
        )
        return renderer.image { context in
            view.drawHierarchy(in: view.bounds, afterScreenUpdates: true)
        }
    }

    /// Saves a UIImage to the photo library.
    ///
    /// Requires the `NSPhotoLibraryAddUsageDescription` key in Info.plist.
    ///
    /// - Parameter image: The image to save.
    public static func saveToPhotoLibrary(_ image: UIImage) {
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
    }

    /// Returns the image data in PNG format.
    ///
    /// - Parameter image: The source image.
    /// - Returns: PNG-encoded data, or nil on failure.
    public static func pngData(_ image: UIImage) -> Data? {
        image.pngData()
    }

    /// Returns the image data in JPEG format.
    ///
    /// - Parameters:
    ///   - image: The source image.
    ///   - quality: JPEG compression quality (0.0 to 1.0). Default 0.9.
    /// - Returns: JPEG-encoded data, or nil on failure.
    public static func jpegData(_ image: UIImage, quality: CGFloat = 0.9) -> Data? {
        image.jpegData(compressionQuality: quality)
    }
}

#endif // os(iOS)
