<!-- category: Fixed -->
- **`VideoNode` and `ViewNode` show their source colours again under the PBR Neutral default.**
  Their materials still pre-applied Filament's `inverseTonemapSRGB()`, which only inverts Filmic,
  so after `createView` switched to PBR Neutral a video or a 2D-in-3D card displayed darker than
  its source (a mid grey came out darker). `video_texture`, `video_texture_chroma_key`,
  `view_texture_lit` and `view_texture_unlit` now invert whichever tone mapper the node's view
  uses: Filmic in a `createARView` view, PBR Neutral otherwise.
- **Bright saturated colours stay saturated in videos, cards and splats.** PBR Neutral desaturates
  highlights, so a colour like `#FF0000` has no exact inverse, and the capped inverse turned it
  into a pastel (255, 212, 212). The shared inverse is still exact for every colour the curve can
  produce. For the others it now balances the peak against the curve's desaturation: red shows
  (235, 13, 13), and no colour lands more than 20 steps (of 255) away.
- **Demo: the 2D-in-3D Inspect card shows its authored swatch colours.** The card is now an unlit
  `ViewNode`; lit, it took the scene light and its amber swatch read as ochre.
