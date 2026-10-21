<!-- category: Fixed -->
- **3D Scan**: the live cloud's overlay view is drawn at ARCore's camera rate (30 fps) instead of the display's, so a mid-range phone no longer renders and swaps a second Filament view for frames whose picture has not changed. The saving orbit keeps the display's full rate (#4743).
