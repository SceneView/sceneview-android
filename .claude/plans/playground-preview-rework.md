# Playground Preview Rework Plan

## Problem
The playground preview shows a random GLB model regardless of the selected example.
"Primitives" shows a Game Boy, "Spring Physics" shows a helmet — completely disconnected.

## Solution: Example-specific preview scenes

### Category 1: Model-based examples (just load the right GLB)
These already work — just need correct defaultModel mapping:
- **Model Viewer** → DamagedHelmet ✅
- **Environment Setup** → SheenChair ✅
- **Camera Controls** → AntiqueCamera ✅
- **Lighting Setup** → DamagedHelmet ✅
- **PBR Materials** → DamagedHelmet ✅
- **Model Animation** → AnimatedAstronaut ✅
- **Multi-Model Scene** → IridescenceLamp ✅
- **Post-Processing** → DamagedHelmet ✅

### Category 2: Geometry examples (create scene in JS via Filament API)
Need custom JS preview functions:
- **Primitives** → Create cube + sphere + cylinder in Filament.js
  - Use `Filament.EntityManager`, `Filament.RenderableManager.Builder`
  - Red cube at (-2,0,0), blue sphere at (0,0,0), green cylinder at (2,0,0)

### Category 3: AR examples (show placeholder)
Cannot run AR in browser. Show a styled placeholder:
- **AR Placement** → Static image/SVG of phone with AR overlay + "AR requires a device" message
- **Face Tracking** → Same pattern
- **Spatial Anchors** → Same pattern

### Category 4: Animation/Physics (load model + auto-animate)
- **Spring Physics** → Load Duck.glb + apply bounce animation in JS
- **Model Animation** → Load Fox.glb or Astronaut + play animation

### Implementation

#### Step 1: Add `previewType` to each example in EXAMPLES database
```javascript
{
  previewType: 'model',        // just load a GLB
  previewType: 'geometry',     // create primitives in JS
  previewType: 'ar-placeholder', // show AR placeholder
  previewType: 'custom',       // custom JS function
}
```

#### Step 2: Add preview builder functions to sceneview.js
```javascript
// sceneview.js additions:
SceneView.prototype.createBox(center, size, color)
SceneView.prototype.createSphere(center, radius, color)
SceneView.prototype.createCylinder(center, radius, height, color)
SceneView.prototype.clearScene()  // remove all entities
```

#### Step 3: Add preview scene builders in playground.html
```javascript
var PREVIEW_SCENES = {
  'primitives': function(sv) {
    sv.clearScene();
    sv.createBox([−2,0,0], [1,1,1], [1,0,0,1]);
    sv.createSphere([0,1,0], 0.5, [0,0,1,1]);
    sv.createCylinder([2,0,0], 0.3, 1.5, [0,1,0,1]);
  },
  'spring-physics': function(sv) {
    sv.loadModel('models/platforms/Duck.glb');
    // bounce animation handled by sv.setAutoRotate or custom
  }
};
```

#### Step 4: AR placeholder
```html
<div class="pg__ar-placeholder" style="display:none">
  <svg><!-- phone with AR overlay illustration --></svg>
  <p>AR features require a physical device</p>
  <a href="https://play.google.com/store/apps/details?id=io.github.sceneview.demo">
    Try on Android →
  </a>
</div>
```

#### Step 5: Wire it all up in loadExample()
```javascript
function loadExample(key) {
  var ex = EXAMPLES[key];
  // ... existing code update ...

  if (ex.previewType === 'ar-placeholder') {
    showARPlaceholder();
  } else if (ex.previewType === 'geometry' && PREVIEW_SCENES[key]) {
    PREVIEW_SCENES[key](sceneViewInstance);
  } else {
    // default: load model
    initSceneView(getModelUrl(ex.defaultModel));
  }
}
```

## Files to modify
1. `website-static/js/sceneview.js` — add createBox/Sphere/Cylinder/clearScene
2. `website-static/playground.html` — add previewType, PREVIEW_SCENES, AR placeholder

## Estimated effort
- sceneview.js geometry API: ~100 lines
- playground.html preview scenes: ~80 lines
- AR placeholder: ~30 lines
- Wiring: ~20 lines
Total: ~230 lines of changes
