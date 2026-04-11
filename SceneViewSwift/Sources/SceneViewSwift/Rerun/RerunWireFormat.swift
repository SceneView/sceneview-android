import Foundation
#if os(iOS)
import ARKit
#endif

/// JSON-lines wire format shared between the Kotlin and Swift Rerun bridges.
///
/// Every event is one JSON object on a single line, terminated by `\n`, and
/// consumed by a Python sidecar that re-logs it via the `rerun-sdk` into the
/// Rerun viewer. The same wire format is emitted on Android (ARCore) and
/// iOS (ARKit), so a single sidecar script handles both platforms.
///
/// Format:
/// ```
/// {"t": 123456789, "type": "camera_pose", "entity": "world/camera", "translation": [x,y,z], "quaternion": [x,y,z,w]}
/// {"t": 123456789, "type": "plane", "entity": "world/planes/<id>", "polygon": [[x,y,z], ...], "kind": "horizontal"}
/// {"t": 123456789, "type": "point_cloud", "entity": "world/points", "positions": [[x,y,z], ...], "confidences": [f, ...]}
/// {"t": 123456789, "type": "anchor", "entity": "world/anchors/<id>", "translation": [x,y,z], "quaternion": [x,y,z,w]}
/// {"t": 123456789, "type": "hit_result", "entity": "world/hits/<id>", "translation": [x,y,z], "distance": f}
/// ```
///
/// This module is pure: no I/O, no threading, no ARKit instances needed by
/// the testable `*Json` overloads — the string-in / string-out nature lets
/// the unit tests assert exact golden JSON output on plain Swift.
///
/// JSON is hand-rolled instead of using `JSONEncoder` so Kotlin and Swift
/// emit byte-identical output for the same input — the Python sidecar
/// doesn't care about whitespace but the cross-platform golden tests do.
public enum RerunWireFormat {

    // MARK: - Testable overloads (primitives only)

    /// Camera pose event — takes the pose as primitives so tests don't need
    /// a real ARKit frame.
    public static func cameraPoseJson(
        timestampNanos: Int64,
        tx: Float, ty: Float, tz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float,
        entity: String = "world/camera"
    ) -> String {
        var s = ""
        s.reserveCapacity(192)
        s.append("{")
        appendCommonHeader(&s, timestampNanos: timestampNanos, type: "camera_pose", entity: entity)
        s.append(",\"translation\":[")
        appendFloat(&s, tx); s.append(",")
        appendFloat(&s, ty); s.append(",")
        appendFloat(&s, tz)
        s.append("],\"quaternion\":[")
        appendFloat(&s, qx); s.append(",")
        appendFloat(&s, qy); s.append(",")
        appendFloat(&s, qz); s.append(",")
        appendFloat(&s, qw)
        s.append("]}\n")
        return s
    }

    /// Anchor event — takes pose as primitives.
    public static func anchorJson(
        timestampNanos: Int64,
        id: Int,
        tx: Float, ty: Float, tz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float
    ) -> String {
        var s = ""
        s.reserveCapacity(192)
        s.append("{")
        appendCommonHeader(&s, timestampNanos: timestampNanos, type: "anchor", entity: "world/anchors/\(id)")
        s.append(",\"translation\":[")
        appendFloat(&s, tx); s.append(",")
        appendFloat(&s, ty); s.append(",")
        appendFloat(&s, tz)
        s.append("],\"quaternion\":[")
        appendFloat(&s, qx); s.append(",")
        appendFloat(&s, qy); s.append(",")
        appendFloat(&s, qz); s.append(",")
        appendFloat(&s, qw)
        s.append("]}\n")
        return s
    }

    /// Hit result event — takes position + distance.
    public static func hitResultJson(
        timestampNanos: Int64,
        id: Int,
        tx: Float, ty: Float, tz: Float,
        distance: Float
    ) -> String {
        var s = ""
        s.reserveCapacity(160)
        s.append("{")
        appendCommonHeader(&s, timestampNanos: timestampNanos, type: "hit_result", entity: "world/hits/\(id)")
        s.append(",\"translation\":[")
        appendFloat(&s, tx); s.append(",")
        appendFloat(&s, ty); s.append(",")
        appendFloat(&s, tz)
        s.append("],\"distance\":")
        appendFloat(&s, distance)
        s.append("}\n")
        return s
    }

    /// Point cloud event — takes already-split positions and confidences.
    /// Positions is a flat `[x0, y0, z0, x1, y1, z1, ...]` buffer; each 3
    /// floats form one point. Confidences is parallel, one per point.
    public static func pointCloudJson(
        timestampNanos: Int64,
        positions: [Float],
        confidences: [Float] = [],
        entity: String = "world/points"
    ) -> String {
        let n = positions.count / 3
        var s = ""
        s.reserveCapacity(64 + n * 48)
        s.append("{")
        appendCommonHeader(&s, timestampNanos: timestampNanos, type: "point_cloud", entity: entity)
        s.append(",\"positions\":[")
        for i in 0..<n {
            if i > 0 { s.append(",") }
            s.append("[")
            appendFloat(&s, positions[i * 3]); s.append(",")
            appendFloat(&s, positions[i * 3 + 1]); s.append(",")
            appendFloat(&s, positions[i * 3 + 2])
            s.append("]")
        }
        s.append("],\"confidences\":[")
        for i in 0..<confidences.count {
            if i > 0 { s.append(",") }
            appendFloat(&s, confidences[i])
        }
        s.append("]}\n")
        return s
    }

    /// Plane event — takes a world-space polygon as a list of 3-float
    /// arrays. ARKit gives us a `simd_float3` array from the plane anchor
    /// geometry; the production `plane(_:timestamp:)` overload flattens
    /// that and calls this helper.
    public static func planeJson(
        timestampNanos: Int64,
        id: Int,
        kind: String,
        worldPolygon: [[Float]]
    ) -> String {
        var s = ""
        s.reserveCapacity(256 + worldPolygon.count * 48)
        s.append("{")
        appendCommonHeader(&s, timestampNanos: timestampNanos, type: "plane", entity: "world/planes/\(id)")
        s.append(",\"kind\":\"")
        s.append(kind)
        s.append("\",\"polygon\":[")
        for (idx, p) in worldPolygon.enumerated() {
            if idx > 0 { s.append(",") }
            s.append("[")
            if p.count >= 3 {
                appendFloat(&s, p[0]); s.append(",")
                appendFloat(&s, p[1]); s.append(",")
                appendFloat(&s, p[2])
            }
            s.append("]")
        }
        s.append("]}\n")
        return s
    }

    // MARK: - Production overloads (ARKit types) — iOS only

    #if os(iOS)

    /// Camera pose event — derives translation and quaternion from an
    /// ARFrame's camera `transform` (`simd_float4x4`).
    public static func cameraPose(
        timestampNanos: Int64,
        camera: ARCamera,
        entity: String = "world/camera"
    ) -> String {
        let t = camera.transform
        // Translation is the 4th column of the 4x4 transform.
        let tx = t.columns.3.x
        let ty = t.columns.3.y
        let tz = t.columns.3.z
        // Rotation quaternion from the upper-left 3x3.
        let q = simd_quatf(
            simd_float3x3(
                SIMD3(t.columns.0.x, t.columns.0.y, t.columns.0.z),
                SIMD3(t.columns.1.x, t.columns.1.y, t.columns.1.z),
                SIMD3(t.columns.2.x, t.columns.2.y, t.columns.2.z)
            )
        )
        return cameraPoseJson(
            timestampNanos: timestampNanos,
            tx: tx, ty: ty, tz: tz,
            qx: q.imag.x, qy: q.imag.y, qz: q.imag.z, qw: q.real,
            entity: entity
        )
    }

    /// Plane event — derives the world-space polygon from an ARPlaneAnchor.
    public static func plane(
        timestampNanos: Int64,
        anchor: ARPlaneAnchor
    ) -> String {
        // ARKit's ARPlaneGeometry.boundaryVertices is in the anchor's local
        // space. Lift each vertex to world space via anchor.transform.
        let xform = anchor.transform
        let verts = anchor.geometry.boundaryVertices
        var worldPoly: [[Float]] = []
        worldPoly.reserveCapacity(verts.count)
        for v in verts {
            let local = SIMD4<Float>(v.x, v.y, v.z, 1.0)
            let world = xform * local
            worldPoly.append([world.x, world.y, world.z])
        }
        let kind: String
        switch anchor.alignment {
        case .horizontal: kind = "horizontal_upward"
        case .vertical: kind = "vertical"
        @unknown default: kind = "unknown"
        }
        return planeJson(
            timestampNanos: timestampNanos,
            id: anchor.identifier.hashValue,
            kind: kind,
            worldPolygon: worldPoly
        )
    }

    /// Point cloud event — derives positions from an ARFrame's raw feature
    /// points. ARKit's `ARPointCloud.points` is `[simd_float3]`, which we
    /// flatten into the `[x, y, z, x, y, z, ...]` shape our testable
    /// overload expects. Confidences are not exposed by ARKit so we emit
    /// an empty list.
    public static func pointCloud(
        timestampNanos: Int64,
        cloud: ARPointCloud,
        entity: String = "world/points"
    ) -> String {
        let pts = cloud.points
        var flat: [Float] = []
        flat.reserveCapacity(pts.count * 3)
        for p in pts {
            flat.append(p.x)
            flat.append(p.y)
            flat.append(p.z)
        }
        return pointCloudJson(
            timestampNanos: timestampNanos,
            positions: flat,
            confidences: [],
            entity: entity
        )
    }

    /// Anchor event — takes any ARAnchor (ARPlaneAnchor is handled by
    /// `plane(_:timestamp:)` instead, this overload is for user-placed
    /// AnchorEntity-backed anchors).
    public static func anchor(
        timestampNanos: Int64,
        arAnchor: ARAnchor
    ) -> String {
        let t = arAnchor.transform
        let tx = t.columns.3.x
        let ty = t.columns.3.y
        let tz = t.columns.3.z
        let q = simd_quatf(
            simd_float3x3(
                SIMD3(t.columns.0.x, t.columns.0.y, t.columns.0.z),
                SIMD3(t.columns.1.x, t.columns.1.y, t.columns.1.z),
                SIMD3(t.columns.2.x, t.columns.2.y, t.columns.2.z)
            )
        )
        return anchorJson(
            timestampNanos: timestampNanos,
            id: arAnchor.identifier.hashValue,
            tx: tx, ty: ty, tz: tz,
            qx: q.imag.x, qy: q.imag.y, qz: q.imag.z, qw: q.real
        )
    }

    #endif

    // MARK: - Helpers

    private static func appendCommonHeader(
        _ s: inout String,
        timestampNanos t: Int64,
        type: String,
        entity: String
    ) {
        s.append("\"t\":")
        s.append(String(t))
        s.append(",\"type\":\"")
        s.append(type)
        s.append("\",\"entity\":\"")
        appendEscaped(&s, entity)
        s.append("\"")
    }

    /// Writes a float as JSON number. Non-finite values (NaN/Infinity)
    /// would break the JSON line; we emit `0` instead so the line stays
    /// parseable — matching the Kotlin bridge's behaviour byte-for-byte.
    ///
    /// Finite floats are rendered by `String(describing:)` which matches
    /// Kotlin's `Float.toString()` output for the same input, so the
    /// golden-JSON tests are directly comparable across the two bridges.
    private static func appendFloat(_ s: inout String, _ f: Float) {
        if !f.isFinite {
            s.append("0")
            return
        }
        s.append(String(describing: f))
    }

    /// Minimal JSON string escaper. Entity paths may legitimately contain
    /// `/` but we escape defensively against quotes and control chars.
    private static func appendEscaped(_ s: inout String, _ str: String) {
        for c in str {
            switch c {
            case "\"": s.append("\\\"")
            case "\\": s.append("\\\\")
            case "\n": s.append("\\n")
            case "\r": s.append("\\r")
            case "\t": s.append("\\t")
            default:
                if let ascii = c.asciiValue, ascii < 0x20 {
                    s.append(String(format: "\\u%04x", ascii))
                } else {
                    s.append(c)
                }
            }
        }
    }
}
