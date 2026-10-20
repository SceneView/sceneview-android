<!-- category: Fixed -->
- **3D Scan (demo)**: a room scan on a Pixel 4a captures points again. That phone stamps each depth image up to 2 ms after the camera frame that hands it over, and the scan threw every one away, so a scan ended with 0 points and no model. A depth up to 5 ms ahead of its frame now counts as that frame's; each estimate is still fused only once (#4729).
