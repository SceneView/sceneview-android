// ─── Concept explainer ────────────────────────────────────────────────────────
//
// Returns a focused explanation of a single Rerun concept, tailored to a
// SceneView / AR developer audience.
export const RERUN_CONCEPTS = [
    "rrd",
    "timelines",
    "entities",
    "archetypes",
    "transforms",
];
const EXPLANATIONS = {
    rrd: `## .rrd — Rerun Data Recording

A **.rrd** file is a binary log of everything that has been streamed into a
Rerun session: every Transform3D, Points3D, Image, Mesh3D, etc., with the
timeline tick it was emitted on. You can think of it as a "flight recorder"
for 3D/AR data.

**For SceneView developers:**
- Generate .rrd offline (Python/Rust/C++ SDK) or at runtime via a sidecar.
- Ship .rrd files next to your sample apps so reviewers can replay a session.
- Host them self-contained: they embed their own schema, no external metadata.
- Load one in the Rerun Web Viewer with \`viewer.start(url, container)\`.

**Gotchas:**
- The .rrd format is not stable across Rerun majors — regenerate when bumping
  the SDK version.
- Files grow fast: a 1-minute AR session can easily be 50-100 MB. Strip point
  clouds or throttle to 10 Hz for docs/demos.`,
    timelines: `## Timelines

A **timeline** is a named axis along which you log data. Every entity can be
logged on multiple timelines simultaneously — the viewer lets you scrub along
any of them.

**Common timelines in a SceneView integration:**
- \`device_clock\` — nanosecond-precision timestamps from ARCore/ARKit frames.
- \`sequence\` — a monotonic frame counter (useful when device clock drifts).
- \`log_tick\` — wall-clock time on the sidecar process.

**Pattern:**
\`\`\`python
rr.set_time_nanos("device_clock", frame_ts_nanos)
rr.log("world/camera", rr.Transform3D(...))
\`\`\`

The wire format used by the SceneView RerunBridge always emits a \`t\` field
in nanoseconds, which the sidecar maps to \`device_clock\` by default.`,
    entities: `## Entities

An **entity path** is a hierarchical name that identifies a logged object:
\`world/camera\`, \`world/planes/floor_12\`, \`world/points\`. Entity paths
form a tree, and transforms on parent entities apply to all children.

**SceneView naming conventions used by the default bridge:**
- \`world/camera\` — the AR camera pose
- \`world/planes/<id>\` — one entity per ARCore Plane
- \`world/points\` — the ARCore point cloud
- \`world/anchors/<id>\` — one entity per user-placed anchor
- \`world/hits/<id>\` — raycast hit results

**Tip:** use short, stable ids. ARCore \`Plane.hashCode()\` is a decent source
of a stable id for the lifetime of the session.`,
    archetypes: `## Archetypes

An **archetype** is the Rerun equivalent of a "widget type" — it tells the
viewer how to render the data you logged. Common archetypes:

| Archetype         | Use case                                  |
|-------------------|-------------------------------------------|
| \`Transform3D\`   | Camera pose, anchor pose, node transform |
| \`Points3D\`      | Point clouds, feature points             |
| \`Mesh3D\`        | Plane polygons, triangulated geometry    |
| \`LineStrips3D\`  | Plane boundaries, trajectories           |
| \`Arrows3D\`      | Normals, forces, gaze vectors            |
| \`Boxes3D\`       | Bounding boxes, AABBs                    |
| \`Image\`         | AR camera feed frames                    |
| \`DepthImage\`    | ARCore depth API output                  |

The wire format defined by the SceneView RerunBridge maps directly to these:
\`camera_pose\` → Transform3D, \`point_cloud\` → Points3D, \`plane\` →
LineStrips3D (or Mesh3D if you triangulate the polygon).`,
    transforms: `## Transforms

Rerun uses a **right-handed** coordinate system by default (X right, Y up, Z
back), consistent with OpenGL and Filament. SceneView and ARCore use the same
convention so you can log a pose verbatim.

**Quaternions** are in \`(x, y, z, w)\` order in Rerun, which matches ARCore's
\`Pose.getRotationQuaternion()\` and ARKit's \`simd_quatf\`.

**Composing transforms:** log a parent entity with a Transform3D once, and all
children inherit it. Example:

\`\`\`python
rr.log("world", rr.Transform3D())  # identity
rr.log("world/camera", rr.Transform3D(
    translation=[0.1, 1.7, -0.2],
    rotation=rr.Quaternion(xyzw=[0, 0, 0, 1]),
))
rr.log("world/camera/image", rr.Image(...))
\`\`\`

Any point logged under \`world/camera/\*\` will be rendered in camera-local
space, so an image frame appears as a floating rectangle attached to the
camera pose.`,
};
export function explainConcept(concept) {
    if (!RERUN_CONCEPTS.includes(concept)) {
        throw new Error(`Unknown concept "${concept}". Valid: ${RERUN_CONCEPTS.join(", ")}`);
    }
    return EXPLANATIONS[concept];
}
