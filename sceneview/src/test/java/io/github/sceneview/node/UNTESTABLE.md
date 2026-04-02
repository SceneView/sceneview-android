# Classes not covered by JVM unit tests

The following classes in the `node` package require Filament JNI (native Engine) and cannot
be tested as pure JVM unit tests.

## NodeGestureDelegate
- Constructor requires `Node`, which requires `Engine` (Filament JNI).
- All gesture callbacks forward to `node.*` properties.
- **Recommendation:** cover with Android instrumented tests or Robolectric with Filament stubs.

## NodeAnimationDelegate
- Constructor requires `Node`, which requires `Engine` (Filament JNI).
- `onFrame()` reads/writes `node.transform` which needs a Filament TransformManager.
- **Recommendation:** cover with Android instrumented tests.

## Node
- Directly creates Filament entities via `EntityManager.get().create()`.
- All transform operations go through `TransformManager`.
- **Recommendation:** cover with Android instrumented tests.

## CollisionSystem
- Constructor requires `com.google.android.filament.View`.
- Hit-testing methods use `view.camera` and `view.screenToRay`.
- The `hitTest(Ray)` overload is the most testable, but still uses `Collider.getTransformedShape()`
  which requires a Node with a Filament transform.
- **Recommendation:** refactor to accept a camera/ray abstraction, or cover with instrumented tests.

## SceneRenderer
- Constructor requires `Engine`, `View`, and `Renderer` (all Filament JNI).
- Manages native surface lifecycle (`UiHelper`, `SwapChain`, `DisplayHelper`).
- **Recommendation:** cover with Android instrumented tests.
