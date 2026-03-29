#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit
import Foundation

/// Gesture types that can be recognized on individual entities.
///
/// Mirrors SceneView Android's gesture detection system — provides per-entity
/// tap, drag, pinch-scale, and rotation gesture handling.
///
/// ```swift
/// let cube = GeometryNode.cube(size: 0.3, color: .blue)
/// NodeGesture.onTap(cube.entity) {
///     print("Cube tapped!")
/// }
/// NodeGesture.onDrag(cube.entity) { translation in
///     cube.position += translation
/// }
/// ```
public enum NodeGesture {

    // MARK: - Gesture state storage

    /// Registry of tap handlers keyed by entity ID.
    @MainActor
    private static var tapHandlers: [ObjectIdentifier: () -> Void] = [:]

    /// Registry of drag handlers keyed by entity ID.
    @MainActor
    private static var dragHandlers: [ObjectIdentifier: (SIMD3<Float>) -> Void] = [:]

    /// Registry of scale handlers keyed by entity ID.
    @MainActor
    private static var scaleHandlers: [ObjectIdentifier: (Float) -> Void] = [:]

    /// Registry of rotation handlers keyed by entity ID.
    @MainActor
    private static var rotateHandlers: [ObjectIdentifier: (Float) -> Void] = [:]

    /// Registry of long press handlers keyed by entity ID.
    @MainActor
    private static var longPressHandlers: [ObjectIdentifier: () -> Void] = [:]

    // MARK: - Registration

    /// Registers a tap handler for an entity.
    ///
    /// The entity must have collision shapes for hit testing.
    ///
    /// - Parameters:
    ///   - entity: The entity to detect taps on.
    ///   - handler: Closure invoked when the entity is tapped.
    @MainActor
    public static func onTap(_ entity: Entity, handler: @escaping () -> Void) {
        ensureCollision(entity)
        tapHandlers[ObjectIdentifier(entity)] = handler
    }

    /// Registers a drag handler for an entity.
    ///
    /// - Parameters:
    ///   - entity: The entity to detect drags on.
    ///   - handler: Closure invoked with the translation delta (in world space).
    @MainActor
    public static func onDrag(_ entity: Entity, handler: @escaping (SIMD3<Float>) -> Void) {
        ensureCollision(entity)
        dragHandlers[ObjectIdentifier(entity)] = handler
    }

    /// Registers a pinch-to-scale handler for an entity.
    ///
    /// - Parameters:
    ///   - entity: The entity to detect scale gestures on.
    ///   - handler: Closure invoked with the magnification factor.
    @MainActor
    public static func onScale(_ entity: Entity, handler: @escaping (Float) -> Void) {
        ensureCollision(entity)
        scaleHandlers[ObjectIdentifier(entity)] = handler
    }

    /// Registers a two-finger rotation handler for an entity.
    ///
    /// - Parameters:
    ///   - entity: The entity to detect rotation gestures on.
    ///   - handler: Closure invoked with the rotation angle in radians.
    @MainActor
    public static func onRotate(_ entity: Entity, handler: @escaping (Float) -> Void) {
        ensureCollision(entity)
        rotateHandlers[ObjectIdentifier(entity)] = handler
    }

    /// Registers a long press handler for an entity.
    ///
    /// - Parameters:
    ///   - entity: The entity to detect long presses on.
    ///   - handler: Closure invoked when the entity is long-pressed.
    @MainActor
    public static func onLongPress(_ entity: Entity, handler: @escaping () -> Void) {
        ensureCollision(entity)
        longPressHandlers[ObjectIdentifier(entity)] = handler
    }

    // MARK: - Deregistration

    /// Removes all gesture handlers for an entity.
    @MainActor
    public static func removeAll(from entity: Entity) {
        let id = ObjectIdentifier(entity)
        tapHandlers.removeValue(forKey: id)
        dragHandlers.removeValue(forKey: id)
        scaleHandlers.removeValue(forKey: id)
        rotateHandlers.removeValue(forKey: id)
        longPressHandlers.removeValue(forKey: id)
    }

    /// Removes all registered gesture handlers.
    @MainActor
    public static func removeAllHandlers() {
        tapHandlers.removeAll()
        dragHandlers.removeAll()
        scaleHandlers.removeAll()
        rotateHandlers.removeAll()
        longPressHandlers.removeAll()
    }

    // MARK: - Dispatch (called by scene implementation)

    /// Dispatches a tap event to the entity's registered handler.
    @MainActor
    public static func dispatchTap(on entity: Entity) {
        tapHandlers[ObjectIdentifier(entity)]?()
    }

    /// Dispatches a drag event to the entity's registered handler.
    @MainActor
    public static func dispatchDrag(on entity: Entity, translation: SIMD3<Float>) {
        dragHandlers[ObjectIdentifier(entity)]?(translation)
    }

    /// Dispatches a scale event to the entity's registered handler.
    @MainActor
    public static func dispatchScale(on entity: Entity, magnification: Float) {
        scaleHandlers[ObjectIdentifier(entity)]?(magnification)
    }

    /// Dispatches a rotation event to the entity's registered handler.
    @MainActor
    public static func dispatchRotate(on entity: Entity, angle: Float) {
        rotateHandlers[ObjectIdentifier(entity)]?(angle)
    }

    /// Dispatches a long press event to the entity's registered handler.
    @MainActor
    public static func dispatchLongPress(on entity: Entity) {
        longPressHandlers[ObjectIdentifier(entity)]?()
    }

    /// Whether the entity has any registered gesture handlers.
    @MainActor
    public static func hasHandlers(for entity: Entity) -> Bool {
        let id = ObjectIdentifier(entity)
        return tapHandlers[id] != nil
            || dragHandlers[id] != nil
            || scaleHandlers[id] != nil
            || rotateHandlers[id] != nil
            || longPressHandlers[id] != nil
    }

    // MARK: - Private helpers

    private static func ensureCollision(_ entity: Entity) {
        if entity.components[CollisionComponent.self] == nil {
            if let modelEntity = entity as? ModelEntity {
                modelEntity.generateCollisionShapes(recursive: true)
            }
        }
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)
