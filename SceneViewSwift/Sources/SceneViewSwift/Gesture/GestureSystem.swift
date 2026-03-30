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

    /// Weak references to registered entities for automatic cleanup.
    @MainActor
    private static var registeredEntities: [ObjectIdentifier: WeakEntity] = [:]

    /// Wrapper for weak entity reference.
    private final class WeakEntity {
        weak var entity: Entity?
        init(_ entity: Entity) { self.entity = entity }
    }

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
        trackEntity(entity)
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
        trackEntity(entity)
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
        trackEntity(entity)
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
        trackEntity(entity)
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
        trackEntity(entity)
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
        registeredEntities.removeValue(forKey: id)
    }

    /// Removes all registered gesture handlers.
    @MainActor
    public static func removeAllHandlers() {
        tapHandlers.removeAll()
        dragHandlers.removeAll()
        scaleHandlers.removeAll()
        rotateHandlers.removeAll()
        longPressHandlers.removeAll()
        registeredEntities.removeAll()
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

    /// Removes handlers for entities that have been deallocated.
    ///
    /// Called automatically during handler registration. Can also be called
    /// manually to reclaim memory in long-running scenes.
    @MainActor
    public static func purgeStaleHandlers() {
        let staleIds = registeredEntities.filter { $0.value.entity == nil }.map { $0.key }
        for id in staleIds {
            tapHandlers.removeValue(forKey: id)
            dragHandlers.removeValue(forKey: id)
            scaleHandlers.removeValue(forKey: id)
            rotateHandlers.removeValue(forKey: id)
            longPressHandlers.removeValue(forKey: id)
            registeredEntities.removeValue(forKey: id)
        }
    }

    private static func ensureCollision(_ entity: Entity) {
        if entity.components[CollisionComponent.self] == nil {
            if let modelEntity = entity as? ModelEntity {
                modelEntity.generateCollisionShapes(recursive: true)
            }
        }
    }

    /// Tracks the entity for automatic stale handler cleanup.
    @MainActor
    private static func trackEntity(_ entity: Entity) {
        let id = ObjectIdentifier(entity)
        if registeredEntities[id] == nil {
            registeredEntities[id] = WeakEntity(entity)
        }
        // Opportunistic cleanup: purge stale entries periodically
        if registeredEntities.count > 10 {
            purgeStaleHandlers()
        }
    }
}

// MARK: - Entity convenience extensions

extension Entity {
    /// Registers a tap handler on this entity. Returns self for chaining.
    ///
    /// ```swift
    /// let cube = GeometryNode.cube(size: 0.3, color: .blue)
    ///     .entity
    ///     .onTap { print("Tapped!") }
    /// ```
    @MainActor
    @discardableResult
    public func onTap(_ handler: @escaping () -> Void) -> Entity {
        NodeGesture.onTap(self, handler: handler)
        return self
    }

    /// Registers a drag handler on this entity. Returns self for chaining.
    @MainActor
    @discardableResult
    public func onDrag(_ handler: @escaping (SIMD3<Float>) -> Void) -> Entity {
        NodeGesture.onDrag(self, handler: handler)
        return self
    }

    /// Registers a scale handler on this entity. Returns self for chaining.
    @MainActor
    @discardableResult
    public func onScale(_ handler: @escaping (Float) -> Void) -> Entity {
        NodeGesture.onScale(self, handler: handler)
        return self
    }

    /// Registers a rotation handler on this entity. Returns self for chaining.
    @MainActor
    @discardableResult
    public func onRotate(_ handler: @escaping (Float) -> Void) -> Entity {
        NodeGesture.onRotate(self, handler: handler)
        return self
    }

    /// Registers a long press handler on this entity. Returns self for chaining.
    @MainActor
    @discardableResult
    public func onLongPress(_ handler: @escaping () -> Void) -> Entity {
        NodeGesture.onLongPress(self, handler: handler)
        return self
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)
