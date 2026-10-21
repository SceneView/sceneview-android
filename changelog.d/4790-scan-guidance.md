<!-- category: Fixed -->
- **Android 3D Scan: the floor plan export no longer hangs on "Measuring the room…" ([#4790](https://github.com/sceneview/sceneview/pull/4790)).** A long measure says it is still measuring and shows the plan when it ends; only a measure that fails says so, and it is tried again the next time.
- **Android 3D Scan: steadier guidance.** The turn hint changes only once the new direction has held for three seconds, "Stuck" no longer shows on the status or the coaching line while points are still being added, and the lasting "Photo limit reached" line is gone.
- **Android 3D Scan: live points without holes.** Walls stay filled in the live view, and points close to the camera stay small instead of covering it.
