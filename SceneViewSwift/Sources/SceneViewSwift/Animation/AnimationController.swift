#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit
import Foundation
import Combine

/// High-level animation controller for managing entity animations.
///
/// Provides a richer animation API than raw RealityKit, with support for
/// playing by name, speed control, blending, and state observation.
///
/// ```swift
/// let model = try await ModelNode.load("robot.usdz")
/// let controller = AnimationController(entity: model.entity)
///
/// // Play by name with speed
/// controller.play("walk", speed: 1.5, loop: true)
///
/// // Crossfade to another animation
/// controller.crossfade(to: "run", duration: 0.3)
///
/// // Observe state changes
/// controller.onStateChanged { state in
///     print("Animation state: \(state)")
/// }
/// ```
public final class AnimationController: @unchecked Sendable {

    /// Current animation playback state.
    public enum State: Sendable, Equatable {
        case idle
        case playing(name: String)
        case paused(name: String)
    }

    /// The entity being animated.
    public let entity: Entity

    /// Current playback state.
    public private(set) var state: State = .idle {
        didSet {
            if state != oldValue {
                stateCallback?(state)
            }
        }
    }

    /// Current playback speed multiplier.
    public var speed: Float = 1.0

    /// The name of the currently active animation, if any.
    public var currentAnimationName: String? {
        switch state {
        case .playing(let name), .paused(let name):
            return name
        case .idle:
            return nil
        }
    }

    /// All available animation names.
    public var availableAnimations: [String] {
        entity.availableAnimations.compactMap { $0.name }
    }

    /// The number of available animations.
    public var animationCount: Int {
        entity.availableAnimations.count
    }

    private var stateCallback: ((State) -> Void)?
    private var activePlaybackController: AnimationPlaybackController?

    /// Creates an animation controller for the given entity.
    ///
    /// - Parameter entity: The entity with animations to control.
    public init(entity: Entity) {
        self.entity = entity
    }

    // MARK: - Playback

    /// Plays an animation by name.
    ///
    /// - Parameters:
    ///   - name: The animation name (as authored in the 3D file).
    ///   - speed: Playback speed multiplier. Default uses the controller's speed property.
    ///   - loop: Whether to loop the animation. Default true.
    ///   - transitionDuration: Blend time in seconds. Default 0.2.
    /// - Returns: True if the animation was found and started.
    @discardableResult
    public func play(
        _ name: String,
        speed: Float? = nil,
        loop: Bool = true,
        transitionDuration: TimeInterval = 0.2
    ) -> Bool {
        guard let animation = entity.availableAnimations.first(where: { $0.name == name }) else {
            return false
        }

        let effectiveSpeed = speed ?? self.speed
        let resource = loop ? animation.repeat() : animation
        let playback = entity.playAnimation(
            resource,
            transitionDuration: transitionDuration,
            startsPaused: false
        )
        playback.speed = effectiveSpeed
        activePlaybackController = playback
        state = .playing(name: name)
        return true
    }

    /// Plays an animation by index.
    ///
    /// - Parameters:
    ///   - index: Zero-based animation index.
    ///   - speed: Playback speed multiplier.
    ///   - loop: Whether to loop. Default true.
    ///   - transitionDuration: Blend time in seconds. Default 0.2.
    /// - Returns: True if the index is valid and animation started.
    @discardableResult
    public func play(
        at index: Int,
        speed: Float? = nil,
        loop: Bool = true,
        transitionDuration: TimeInterval = 0.2
    ) -> Bool {
        guard index < entity.availableAnimations.count else { return false }
        let animation = entity.availableAnimations[index]
        let name = animation.name ?? "Animation_\(index)"

        let effectiveSpeed = speed ?? self.speed
        let resource = loop ? animation.repeat() : animation
        let playback = entity.playAnimation(
            resource,
            transitionDuration: transitionDuration,
            startsPaused: false
        )
        playback.speed = effectiveSpeed
        activePlaybackController = playback
        state = .playing(name: name)
        return true
    }

    /// Plays all animations simultaneously.
    ///
    /// - Parameters:
    ///   - loop: Whether to loop. Default true.
    ///   - speed: Playback speed. Default uses controller speed.
    public func playAll(loop: Bool = true, speed: Float? = nil) {
        let effectiveSpeed = speed ?? self.speed
        for animation in entity.availableAnimations {
            let resource = loop ? animation.repeat() : animation
            let playback = entity.playAnimation(
                resource,
                transitionDuration: 0,
                startsPaused: false
            )
            playback.speed = effectiveSpeed
        }
        state = .playing(name: "all")
    }

    /// Crossfades from the current animation to a new one.
    ///
    /// - Parameters:
    ///   - name: Target animation name.
    ///   - duration: Crossfade duration in seconds. Default 0.3.
    ///   - loop: Whether the target animation loops. Default true.
    /// - Returns: True if the target animation was found.
    @discardableResult
    public func crossfade(
        to name: String,
        duration: TimeInterval = 0.3,
        loop: Bool = true
    ) -> Bool {
        play(name, loop: loop, transitionDuration: duration)
    }

    /// Stops all animations and resets to idle.
    public func stop() {
        entity.stopAllAnimations()
        activePlaybackController = nil
        state = .idle
    }

    /// Pauses the current animation.
    public func pause() {
        activePlaybackController?.pause()
        if case .playing(let name) = state {
            state = .paused(name: name)
        }
    }

    /// Resumes a paused animation.
    public func resume() {
        activePlaybackController?.resume()
        if case .paused(let name) = state {
            state = .playing(name: name)
        }
    }

    // MARK: - State Observation

    /// Registers a callback for animation state changes.
    ///
    /// - Parameter callback: Closure invoked when state changes.
    public func onStateChanged(_ callback: @escaping (State) -> Void) {
        self.stateCallback = callback
    }

    // MARK: - Speed Control

    /// Updates the playback speed of the active animation.
    ///
    /// - Parameter newSpeed: New speed multiplier.
    public func setSpeed(_ newSpeed: Float) {
        self.speed = newSpeed
        activePlaybackController?.speed = newSpeed
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)
