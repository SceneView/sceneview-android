<!-- category: Fixed -->
- Cinematic camera moves (`CameraAnimator` through `CinematicCameraManipulator`) no longer jump back to the gesture camera when a reveal or `flyTo` ends, start on a scene that had stopped rendering, reach every keyframe at its declared time, and loop a fractional-turn tour without an elevation jump; a touch also stops the gesture camera still coasting behind a cinematic.
