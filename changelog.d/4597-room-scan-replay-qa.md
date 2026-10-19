<!-- category: Fixed -->
- **Android demo: the 3D Scan replay reads at a glance ([#4597](https://github.com/sceneview/sceneview/issues/4597)).**
  The camera path is one blue, faint to strong, instead of a purple-to-blue ramp, and it ends on the frustum's colour: path and photos read as the camera's. The settings sheet is the legend — each layer row draws what it shows (the path's stroke, a frustum, a floor and a wall, a few points) over a line that says it. The camera's photos have their own switch, apart from the path, so a long recording's frusta can be hidden; the Anchors row, which a room scan never fills, is gone.
  The Camera view draws the reconstructed points and planes over each recorded frame, seen from that frame's pose through the recording's lens.
