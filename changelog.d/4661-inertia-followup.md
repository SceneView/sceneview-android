<!-- category: Fixed -->
- A node that accepts a drag, twist or pinch by overriding `onMoveBegin` / `onRotateBegin` / `onScaleBegin` — such as the anchor `AutoPlacementScene` lets you adjust — now coasts after release like any other editable node, instead of stopping dead.
- A FOV pinch no longer crashes with `AbstractMethodError` when the inner camera manipulator is a Java class compiled before `stopInertia()` existed.
