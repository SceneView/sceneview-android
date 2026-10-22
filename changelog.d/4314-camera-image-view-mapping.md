<!-- category: Fixed -->
- **Demo app: ML Object Label places each label from where the camera was when the object was seen.** A label is anchored where a world ray hits, cast from the camera pose and intrinsics captured with the analysed image through the detected pixel, so its placement no longer depends on how far the phone moved while the detector ran. The detector's input rotation now follows the real sensor orientation, and objects are tracked by ML Kit `trackingId`. Leaving the demo no longer detaches anchors while the ARCore session is closing.

<!-- category: Added -->
- **Android AR detector-to-image mapping.** `DetectorImageMapping(imageSize, inputRotationDegrees).detectorPixelToImagePixel(x, y)` maps a vision API's rotated output pixel back to the unrotated CPU-image pixel that `camera.imageIntrinsics` describes, the pixel to cast a world ray through.
