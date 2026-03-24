#if os(iOS) || os(macOS) || os(visionOS)

// MARK: - Float clamping helper

/// Clamps a `Float` value to the given closed range.
internal extension Float {
    func clamped(to range: ClosedRange<Float>) -> Float {
        return Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)
