<!-- category: Fixed -->
- **`VideoNode` and `ViewNode` show their source colours again under the PBR Neutral default.**
  Their materials still pre-applied Filament's `inverseTonemapSRGB()`, which only inverts Filmic,
  so after `createView` switched to PBR Neutral a video or a 2D-in-3D card displayed darker than
  its source (a mid grey came out darker). `video_texture`, `video_texture_chroma_key`,
  `view_texture_lit` and `view_texture_unlit` now invert whichever tone mapper the node's view
  uses: Filmic in a `createARView` view, PBR Neutral otherwise. The PBR Neutral inverse is capped
  the same way as the splat material, so near-white stays bounded.
- **Demo: AR Fog renders through the AR view.** It passed `rememberView(engine)` to
  `ARSceneView`, so its camera feed, which the camera-stream materials pre-distort for Filmic,
  went through PBR Neutral instead and came out over-bright.
