/**
 * SceneView Web — Simple 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * Powered by Filament.js v1.70.1 (Google's PBR renderer, WASM).
 * https://sceneview.github.io
 *
 * Features:
 *   - Model loading (glTF/GLB)
 *   - Camera orbit with inertia
 *   - Ray casting & hit testing
 *   - Collision shapes (AABB, sphere)
 *   - Gesture system (tap, double-tap, long-press, drag, pinch, rotate)
 *   - Entity selection with visual feedback
 *   - Draggable entities with axis constraints
 *
 * @version 1.4.0
 * @license MIT
 */
(function(global) {
  'use strict';

  // Filament.js is loaded via <script> tag in HTML (js/filament/filament.js)
  // This avoids dynamic script injection issues with WASM resolution.

  /**
   * Wait for Filament to be available (loaded by the script tag).
   */
  function _ensureFilament() {
    return new Promise(function(resolve, reject) {
      if (typeof Filament !== 'undefined') { resolve(); return; }
      // Poll briefly in case the script tag hasn't finished loading
      var attempts = 0;
      var check = setInterval(function() {
        if (typeof Filament !== 'undefined') { clearInterval(check); resolve(); }
        if (++attempts > 100) { clearInterval(check); reject(new Error('SceneView: Filament.js not loaded')); }
      }, 50);
    });
  }

  // =========================================================================
  // Vec3 — lightweight 3-component vector math (pure JS, no dependencies)
  // =========================================================================

  var Vec3 = {
    create: function(x, y, z) { return [x || 0, y || 0, z || 0]; },
    add: function(a, b) { return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]; },
    sub: function(a, b) { return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]; },
    scale: function(v, s) { return [v[0] * s, v[1] * s, v[2] * s]; },
    dot: function(a, b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; },
    cross: function(a, b) {
      return [
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
      ];
    },
    length: function(v) { return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]); },
    normalize: function(v) {
      var len = Vec3.length(v);
      return len > 1e-8 ? [v[0] / len, v[1] / len, v[2] / len] : [0, 0, 0];
    },
    distance: function(a, b) { return Vec3.length(Vec3.sub(a, b)); },
    lerp: function(a, b, t) {
      return [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
    },
    min: function(a, b) { return [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])]; },
    max: function(a, b) { return [Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])]; },
    negate: function(v) { return [-v[0], -v[1], -v[2]]; }
  };

  // =========================================================================
  // Mat4 — 4x4 matrix utilities (column-major, OpenGL convention)
  // =========================================================================

  var Mat4 = {
    /** Create identity matrix */
    identity: function() {
      return [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1];
    },

    /** Multiply two 4x4 column-major matrices: result = a * b */
    multiply: function(a, b) {
      var r = new Array(16);
      for (var c = 0; c < 4; c++) {
        for (var row = 0; row < 4; row++) {
          r[c * 4 + row] =
            a[0 * 4 + row] * b[c * 4 + 0] +
            a[1 * 4 + row] * b[c * 4 + 1] +
            a[2 * 4 + row] * b[c * 4 + 2] +
            a[3 * 4 + row] * b[c * 4 + 3];
        }
      }
      return r;
    },

    /** Transform a vec4 by a 4x4 matrix: result = M * v */
    mulVec4: function(m, v) {
      return [
        m[0] * v[0] + m[4] * v[1] + m[8]  * v[2] + m[12] * v[3],
        m[1] * v[0] + m[5] * v[1] + m[9]  * v[2] + m[13] * v[3],
        m[2] * v[0] + m[6] * v[1] + m[10] * v[2] + m[14] * v[3],
        m[3] * v[0] + m[7] * v[1] + m[11] * v[2] + m[15] * v[3]
      ];
    },

    /** Invert a 4x4 matrix. Returns null if singular. */
    invert: function(m) {
      var inv = new Array(16);
      inv[0]  =  m[5]*m[10]*m[15] - m[5]*m[11]*m[14] - m[9]*m[6]*m[15] + m[9]*m[7]*m[14] + m[13]*m[6]*m[11] - m[13]*m[7]*m[10];
      inv[4]  = -m[4]*m[10]*m[15] + m[4]*m[11]*m[14] + m[8]*m[6]*m[15] - m[8]*m[7]*m[14] - m[12]*m[6]*m[11] + m[12]*m[7]*m[10];
      inv[8]  =  m[4]*m[9]*m[15]  - m[4]*m[11]*m[13] - m[8]*m[5]*m[15] + m[8]*m[7]*m[13] + m[12]*m[5]*m[11] - m[12]*m[7]*m[9];
      inv[12] = -m[4]*m[9]*m[14]  + m[4]*m[10]*m[13] + m[8]*m[5]*m[14] - m[8]*m[6]*m[13] - m[12]*m[5]*m[10] + m[12]*m[6]*m[9];
      inv[1]  = -m[1]*m[10]*m[15] + m[1]*m[11]*m[14] + m[9]*m[2]*m[15] - m[9]*m[3]*m[14] - m[13]*m[2]*m[11] + m[13]*m[3]*m[10];
      inv[5]  =  m[0]*m[10]*m[15] - m[0]*m[11]*m[14] - m[8]*m[2]*m[15] + m[8]*m[3]*m[14] + m[12]*m[2]*m[11] - m[12]*m[3]*m[10];
      inv[9]  = -m[0]*m[9]*m[15]  + m[0]*m[11]*m[13] + m[8]*m[1]*m[15] - m[8]*m[3]*m[13] - m[12]*m[1]*m[11] + m[12]*m[3]*m[9];
      inv[13] =  m[0]*m[9]*m[14]  - m[0]*m[10]*m[13] - m[8]*m[1]*m[14] + m[8]*m[2]*m[13] + m[12]*m[1]*m[10] - m[12]*m[2]*m[9];
      inv[2]  =  m[1]*m[6]*m[15]  - m[1]*m[7]*m[14]  - m[5]*m[2]*m[15] + m[5]*m[3]*m[14] + m[13]*m[2]*m[7]  - m[13]*m[3]*m[6];
      inv[6]  = -m[0]*m[6]*m[15]  + m[0]*m[7]*m[14]  + m[4]*m[2]*m[15] - m[4]*m[3]*m[14] - m[12]*m[2]*m[7]  + m[12]*m[3]*m[6];
      inv[10] =  m[0]*m[5]*m[15]  - m[0]*m[7]*m[13]  - m[4]*m[1]*m[15] + m[4]*m[3]*m[13] + m[12]*m[1]*m[7]  - m[12]*m[3]*m[5];
      inv[14] = -m[0]*m[5]*m[14]  + m[0]*m[6]*m[13]  + m[4]*m[1]*m[14] - m[4]*m[2]*m[13] - m[12]*m[1]*m[6]  + m[12]*m[2]*m[5];
      inv[3]  = -m[1]*m[6]*m[11]  + m[1]*m[7]*m[10]  + m[5]*m[2]*m[11] - m[5]*m[3]*m[10] - m[9]*m[2]*m[7]   + m[9]*m[3]*m[6];
      inv[7]  =  m[0]*m[6]*m[11]  - m[0]*m[7]*m[10]  - m[4]*m[2]*m[11] + m[4]*m[3]*m[10] + m[8]*m[2]*m[7]   - m[8]*m[3]*m[6];
      inv[11] = -m[0]*m[5]*m[11]  + m[0]*m[7]*m[9]   + m[4]*m[1]*m[11] - m[4]*m[3]*m[9]  - m[8]*m[1]*m[7]   + m[8]*m[3]*m[5];
      inv[15] =  m[0]*m[5]*m[10]  - m[0]*m[6]*m[9]   - m[4]*m[1]*m[10] + m[4]*m[2]*m[9]  + m[8]*m[1]*m[6]   - m[8]*m[2]*m[5];

      var det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
      if (Math.abs(det) < 1e-12) return null;

      var invDet = 1.0 / det;
      for (var i = 0; i < 16; i++) inv[i] *= invDet;
      return inv;
    },

    /** Build a perspective projection matrix (column-major). */
    perspective: function(fovYRadians, aspect, near, far) {
      var f = 1.0 / Math.tan(fovYRadians / 2);
      var nf = 1.0 / (near - far);
      return [
        f / aspect, 0, 0, 0,
        0, f, 0, 0,
        0, 0, (far + near) * nf, -1,
        0, 0, 2 * far * near * nf, 0
      ];
    },

    /** Build a lookAt view matrix (column-major). */
    lookAt: function(eye, target, up) {
      var z = Vec3.normalize(Vec3.sub(eye, target));
      var x = Vec3.normalize(Vec3.cross(up, z));
      var y = Vec3.cross(z, x);
      return [
        x[0], y[0], z[0], 0,
        x[1], y[1], z[1], 0,
        x[2], y[2], z[2], 0,
        -Vec3.dot(x, eye), -Vec3.dot(y, eye), -Vec3.dot(z, eye), 1
      ];
    }
  };

  // =========================================================================
  // Collision — ray casting, AABB, sphere intersection (port of KMP core)
  // =========================================================================

  var Collision = {
    /**
     * Ray-AABB intersection (slab method).
     * @param {Array} origin Ray origin [x,y,z]
     * @param {Array} dir Ray direction [x,y,z] (need not be normalized)
     * @param {Array} boxMin AABB minimum corner [x,y,z]
     * @param {Array} boxMax AABB maximum corner [x,y,z]
     * @returns {{ distance: number, point: Array }|null} Hit info or null
     */
    rayAABB: function(origin, dir, boxMin, boxMax) {
      var tMin = -Infinity;
      var tMax = Infinity;

      for (var i = 0; i < 3; i++) {
        if (Math.abs(dir[i]) < 1e-12) {
          // Ray is parallel to this slab — check if origin is inside
          if (origin[i] < boxMin[i] || origin[i] > boxMax[i]) return null;
        } else {
          var invD = 1.0 / dir[i];
          var t1 = (boxMin[i] - origin[i]) * invD;
          var t2 = (boxMax[i] - origin[i]) * invD;
          if (t1 > t2) { var tmp = t1; t1 = t2; t2 = tmp; }
          tMin = Math.max(tMin, t1);
          tMax = Math.min(tMax, t2);
          if (tMin > tMax) return null;
        }
      }

      // Box is behind the ray
      if (tMax < 0) return null;

      var dist = tMin >= 0 ? tMin : tMax;
      return {
        distance: dist,
        point: Vec3.add(origin, Vec3.scale(dir, dist))
      };
    },

    /**
     * Ray-sphere intersection (quadratic formula).
     * @param {Array} origin Ray origin [x,y,z]
     * @param {Array} dir Ray direction [x,y,z] (need not be normalized)
     * @param {Array} center Sphere center [x,y,z]
     * @param {number} radius Sphere radius
     * @returns {{ distance: number, point: Array }|null} Hit info or null
     */
    raySphere: function(origin, dir, center, radius) {
      var oc = Vec3.sub(origin, center);
      var a = Vec3.dot(dir, dir);
      var b = 2.0 * Vec3.dot(oc, dir);
      var c = Vec3.dot(oc, oc) - radius * radius;
      var discriminant = b * b - 4 * a * c;

      if (discriminant < 0) return null;

      var sqrtD = Math.sqrt(discriminant);
      var t1 = (-b - sqrtD) / (2 * a);
      var t2 = (-b + sqrtD) / (2 * a);

      // Both intersections behind the ray
      if (t1 < 0 && t2 < 0) return null;

      var dist;
      if (t1 < 0) dist = t2;       // Ray starts inside sphere
      else dist = t1;               // Nearest intersection

      return {
        distance: dist,
        point: Vec3.add(origin, Vec3.scale(dir, dist))
      };
    },

    /**
     * Ray-plane intersection.
     * @param {Array} origin Ray origin [x,y,z]
     * @param {Array} dir Ray direction [x,y,z]
     * @param {Array} planeNormal Plane normal [x,y,z]
     * @param {number} planeD Plane distance from origin (dot(normal, pointOnPlane))
     * @returns {{ distance: number, point: Array }|null}
     */
    rayPlane: function(origin, dir, planeNormal, planeD) {
      var denom = Vec3.dot(dir, planeNormal);
      if (Math.abs(denom) < 1e-8) return null;
      var t = (planeD - Vec3.dot(origin, planeNormal)) / denom;
      if (t < 0) return null;
      return {
        distance: t,
        point: Vec3.add(origin, Vec3.scale(dir, t))
      };
    },

    /**
     * AABB-AABB overlap test.
     * @param {Array} minA [x,y,z]
     * @param {Array} maxA [x,y,z]
     * @param {Array} minB [x,y,z]
     * @param {Array} maxB [x,y,z]
     * @returns {boolean}
     */
    aabbOverlap: function(minA, maxA, minB, maxB) {
      return minA[0] <= maxB[0] && maxA[0] >= minB[0] &&
             minA[1] <= maxB[1] && maxA[1] >= minB[1] &&
             minA[2] <= maxB[2] && maxA[2] >= minB[2];
    },

    /**
     * Sphere-sphere overlap test.
     * @param {Array} cA Center A [x,y,z]
     * @param {number} rA Radius A
     * @param {Array} cB Center B [x,y,z]
     * @param {number} rB Radius B
     * @returns {boolean}
     */
    sphereOverlap: function(cA, rA, cB, rB) {
      var d = Vec3.distance(cA, cB);
      return d <= rA + rB;
    },

    /**
     * Sphere-AABB overlap test (closest-point on AABB method).
     * @param {Array} center Sphere center [x,y,z]
     * @param {number} radius Sphere radius
     * @param {Array} boxMin AABB min [x,y,z]
     * @param {Array} boxMax AABB max [x,y,z]
     * @returns {boolean}
     */
    sphereAABBOverlap: function(center, radius, boxMin, boxMax) {
      var closest = [
        Math.max(boxMin[0], Math.min(center[0], boxMax[0])),
        Math.max(boxMin[1], Math.min(center[1], boxMax[1])),
        Math.max(boxMin[2], Math.min(center[2], boxMax[2]))
      ];
      var d = Vec3.distance(center, closest);
      return d <= radius;
    }
  };

  // =========================================================================
  // SceneView instance — wraps Filament engine, scene, camera, renderer,
  // collision system, and gesture handling.
  // =========================================================================

  class SceneViewInstance {
    constructor(canvas, engine, scene, renderer, view, swapChain, camera, cameraEntity, loader) {
      this._canvas = canvas;
      this._engine = engine;
      this._scene = scene;
      this._renderer = renderer;
      this._view = view;
      this._swapChain = swapChain;
      this._camera = camera;
      this._cameraEntity = cameraEntity;
      this._loader = loader;
      this._asset = null;
      this._angle = 0.785; // Start at ~45° like model-viewer
      this._autoRotate = true;
      this._orbitRadius = 3.5;
      this._orbitHeight = 0.8;
      this._orbitTarget = [0, 0, 0];
      this._running = true;
      this._isDragging = false;
      this._lastMouse = { x: 0, y: 0 };
      // Inertia for smooth orbit deceleration
      this._velocityAngle = 0;
      this._velocityHeight = 0;
      this._dampingFactor = 0.95;
      this._wantsAutoRotate = true; // Remember initial preference for resume after drag
      this._autoRotateTimer = null;

      // --- Collision system ---
      // Maps entity id -> { type: 'box'|'sphere', entity, ... shape params }
      this._colliders = new Map();
      // Entity bounding boxes cache: entity id -> { min: [x,y,z], max: [x,y,z] }
      this._entityBounds = new Map();

      // --- Gesture system ---
      this._gestureCallbacks = {
        tap: [],
        doubleTap: [],
        longPress: [],
        drag: new Map(),        // entity -> { onStart, onMove, onEnd }
        pinch: [],
        rotate: [],
        select: []
      };
      this._selectableEntities = new Set();
      this._selectedEntities = new Set();
      this._draggableEntities = new Map(); // entity -> { axis, snap, bounds }
      this._activeDragEntity = null;
      this._activeDragPlane = null;        // { normal, d } for the drag constraint plane
      this._activeDragLastWorld = null;

      // Pointer tracking for gesture detection
      this._pointers = new Map();          // pointerId -> { x, y, startX, startY, startTime }
      this._lastTapTime = 0;
      this._lastTapPos = { x: 0, y: 0 };
      this._longPressTimer = null;
      this._longPressDuration = 500;
      this._tapDistanceThreshold = 8;      // px — movement beyond this = drag, not tap
      this._doubleTapInterval = 300;       // ms
      this._gestureConsumed = false;       // true when an entity gesture consumed the pointer
      this._pinchStartDist = 0;
      this._rotateStartAngle = 0;

      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

    // =====================================================================
    // Model loading
    // =====================================================================

    /** Load a glTF/GLB model from URL */
    loadModel(url) {
      var self = this;
      return new Promise(function(resolve, reject) {
        fetch(url)
          .then(function(resp) { return resp.arrayBuffer(); })
          .then(function(buffer) {
            Filament.assets = Filament.assets || {};
            Filament.assets[url] = new Uint8Array(buffer);
            try {
              self._showModel(url);
              resolve(self);
            } catch (e) {
              reject(e);
            }
          })
          .catch(reject);
      });
    }

    _showModel(url) {
      // Remove previous model
      if (this._asset) {
        try {
          var self = this;
          this._asset.getRenderableEntities().forEach(function(e) { self._scene.remove(e); });
          this._scene.remove(this._asset.getRoot());
        } catch (e) { /* ignore cleanup errors */ }
        this._asset = null;
        this._entityBounds.clear();
      }

      var data = Filament.assets[url];
      if (!data) throw new Error('Failed to fetch model: ' + url);

      var asset = this._loader.createAsset(data);
      if (!asset) throw new Error('Failed to parse model: ' + url);

      asset.loadResources();
      this._scene.addEntity(asset.getRoot());
      this._scene.addEntities(asset.getRenderableEntities());
      this._asset = asset;

      // Compute and cache bounding boxes for each renderable entity
      this._cacheEntityBounds();

      // Auto-frame the model
      try {
        var bbox = asset.getBoundingBox();
        var cx = (bbox.min[0] + bbox.max[0]) / 2;
        var cy = (bbox.min[1] + bbox.max[1]) / 2;
        var cz = (bbox.min[2] + bbox.max[2]) / 2;
        var sx = bbox.max[0] - bbox.min[0];
        var sy = bbox.max[1] - bbox.min[1];
        var sz = bbox.max[2] - bbox.min[2];
        var maxDim = Math.max(sx, sy, sz);
        if (maxDim > 0) {
          this._orbitTarget = [cx, cy, cz];
          // Tighter framing than before (1.8x instead of 2.5x)
          this._orbitRadius = maxDim * 1.8;
          this._orbitHeight = cy;
        }
      } catch (e) { /* use defaults */ }
    }

    /** Add a model to the scene (without removing existing ones) */
    addModel(url) {
      var self = this;
      return new Promise(function(resolve, reject) {
        fetch(url)
          .then(function(resp) { return resp.arrayBuffer(); })
          .then(function(buffer) {
            var data = new Uint8Array(buffer);
            try {
              var asset = self._loader.createAsset(data);
              if (!asset) { reject(new Error('Failed to parse: ' + url)); return; }
              asset.loadResources();
              self._scene.addEntity(asset.getRoot());
              self._scene.addEntities(asset.getRenderableEntities());
              resolve(asset);
            } catch (e) { reject(e); }
          })
          .catch(reject);
      });
    }

    /** Load a GLB from a Uint8Array buffer directly */
    loadGLBBuffer(buffer, key) {
      var asset = this._loader.createAsset(buffer);
      if (!asset) return null;
      asset.loadResources();
      this._scene.addEntity(asset.getRoot());
      this._scene.addEntities(asset.getRenderableEntities());
      return asset;
    }

    /** Remove an asset from the scene */
    removeAsset(asset) {
      if (!asset) return;
      try {
        var self = this;
        asset.getRenderableEntities().forEach(function(e) { self._scene.remove(e); });
        this._scene.remove(asset.getRoot());
      } catch (e) { /* ignore cleanup errors */ }
    }

    /** Cache bounding boxes for all renderable entities in the current asset */
    _cacheEntityBounds() {
      if (!this._asset) return;
      try {
        var entities = this._asset.getRenderableEntities();
        var rm = this._engine.getRenderableManager();
        for (var i = 0; i < entities.length; i++) {
          var entity = entities[i];
          try {
            var inst = rm.getInstance(entity);
            if (inst) {
              var aabb = rm.getAxisAlignedBoundingBox(inst);
              if (aabb) {
                this._entityBounds.set(entity, {
                  min: [aabb.min[0], aabb.min[1], aabb.min[2]],
                  max: [aabb.max[0], aabb.max[1], aabb.max[2]]
                });
              }
            }
          } catch (e) { /* some entities may not have renderables */ }
        }
      } catch (e) {
        // If RenderableManager API differs, try asset-level bounding box
        try {
          var bbox = this._asset.getBoundingBox();
          var root = this._asset.getRoot();
          this._entityBounds.set(root, {
            min: [bbox.min[0], bbox.min[1], bbox.min[2]],
            max: [bbox.max[0], bbox.max[1], bbox.max[2]]
          });
        } catch (e2) { /* no bounds available */ }
      }
    }

    /** Access engine for advanced Filament operations */
    get engine() { return this._engine; }
    get scene() { return this._scene; }

    setAutoRotate(enabled) { this._autoRotate = enabled; this._wantsAutoRotate = enabled; return this; }
    setCameraDistance(d) { this._orbitRadius = d; return this; }

    setBackgroundColor(r, g, b, a) {
      this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true });
      return this;
    }

    dispose() {
      this._running = false;
      if (this._resizeObserver) this._resizeObserver.disconnect();
      this._colliders.clear();
      this._entityBounds.clear();
      this._selectableEntities.clear();
      this._selectedEntities.clear();
      this._draggableEntities.clear();
      this._pointers.clear();
      try { Filament.Engine.destroy(this._engine); } catch (e) { /* already destroyed */ }
    }

    // =====================================================================
    // Ray casting — screen-to-world ray unprojection
    // =====================================================================

    /**
     * Get the current camera matrices (view and projection) computed from
     * orbit parameters, matching the render loop exactly.
     * @returns {{ view: Array, projection: Array, eye: Array }}
     */
    _getCameraMatrices() {
      var t = this._orbitTarget;
      var r = this._orbitRadius;
      var h = this._orbitHeight;
      var eye = [
        t[0] + Math.sin(this._angle) * r,
        h,
        t[2] + Math.cos(this._angle) * r
      ];
      var fovRad = (this._fov || 45) * Math.PI / 180;
      var aspect = this._canvas.width / this._canvas.height;
      var projection = Mat4.perspective(fovRad, aspect, 0.1, 1000);
      var view = Mat4.lookAt(eye, t, [0, 1, 0]);
      return { view: view, projection: projection, eye: eye };
    }

    /**
     * Convert screen pixel coordinates to a world-space ray.
     * Uses the same camera state as the current render frame.
     *
     * @param {number} screenX Pixel X (relative to canvas, e.g. from event.offsetX)
     * @param {number} screenY Pixel Y (relative to canvas, e.g. from event.offsetY)
     * @returns {{ origin: Array, direction: Array }} World-space ray
     */
    screenToRay(screenX, screenY) {
      var cam = this._getCameraMatrices();
      var canvas = this._canvas;

      // Normalize screen coordinates to NDC [-1, 1]
      var ndcX = (screenX / canvas.clientWidth) * 2 - 1;
      var ndcY = 1 - (screenY / canvas.clientHeight) * 2; // Y is flipped

      // Inverse view-projection matrix
      var vp = Mat4.multiply(cam.projection, cam.view);
      var vpInv = Mat4.invert(vp);
      if (!vpInv) {
        return { origin: cam.eye, direction: [0, 0, -1] };
      }

      // Unproject near point (z = -1 in NDC) and far point (z = 1)
      var nearClip = Mat4.mulVec4(vpInv, [ndcX, ndcY, -1, 1]);
      var farClip  = Mat4.mulVec4(vpInv, [ndcX, ndcY,  1, 1]);

      // Perspective divide
      var nearWorld = [nearClip[0] / nearClip[3], nearClip[1] / nearClip[3], nearClip[2] / nearClip[3]];
      var farWorld  = [farClip[0]  / farClip[3],  farClip[1]  / farClip[3],  farClip[2]  / farClip[3]];

      var direction = Vec3.normalize(Vec3.sub(farWorld, nearWorld));
      return { origin: nearWorld, direction: direction };
    }

    // =====================================================================
    // Hit testing — ray vs entity bounding boxes & custom colliders
    // =====================================================================

    /**
     * Perform a hit test against all entities at the given screen coordinates.
     * Tests custom colliders first, then falls back to entity bounding boxes.
     *
     * @param {number} screenX Pixel X (offsetX from canvas)
     * @param {number} screenY Pixel Y (offsetY from canvas)
     * @returns {{ entity: number, point: Array, distance: number }|null} Nearest hit or null
     */
    hitTest(screenX, screenY) {
      var hits = this.hitTestAll(screenX, screenY);
      return hits.length > 0 ? hits[0] : null;
    }

    /**
     * Perform a hit test returning ALL intersected entities, sorted by distance.
     *
     * @param {number} screenX Pixel X
     * @param {number} screenY Pixel Y
     * @returns {Array<{ entity: number, point: Array, distance: number }>}
     */
    hitTestAll(screenX, screenY) {
      var ray = this.screenToRay(screenX, screenY);
      var hits = [];

      // 1. Test custom colliders (setCollisionBox / setCollisionSphere)
      this._colliders.forEach(function(collider, entityId) {
        var hit = null;
        if (collider.type === 'box') {
          hit = Collision.rayAABB(
            ray.origin, ray.direction,
            collider.min, collider.max
          );
        } else if (collider.type === 'sphere') {
          hit = Collision.raySphere(
            ray.origin, ray.direction,
            collider.center, collider.radius
          );
        }
        if (hit) {
          hits.push({ entity: entityId, point: hit.point, distance: hit.distance });
        }
      });

      // 2. Test entity bounding boxes (from loaded models)
      var self = this;
      this._entityBounds.forEach(function(bounds, entity) {
        // Skip if this entity already has a custom collider
        if (self._colliders.has(entity)) return;
        var hit = Collision.rayAABB(ray.origin, ray.direction, bounds.min, bounds.max);
        if (hit) {
          hits.push({ entity: entity, point: hit.point, distance: hit.distance });
        }
      });

      // Sort by distance (nearest first)
      hits.sort(function(a, b) { return a.distance - b.distance; });
      return hits;
    }

    // =====================================================================
    // Collision shapes — custom AABB and sphere colliders
    // =====================================================================

    /**
     * Set an axis-aligned bounding box collider on an entity.
     * @param {number} entity Filament entity ID
     * @param {Array} halfExtents [hx, hy, hz] half-sizes in each axis
     * @param {Array} [center] Optional center offset [x,y,z], defaults to [0,0,0]
     * @returns {this}
     */
    setCollisionBox(entity, halfExtents, center) {
      center = center || [0, 0, 0];
      this._colliders.set(entity, {
        type: 'box',
        entity: entity,
        halfExtents: halfExtents,
        center: center,
        min: Vec3.sub(center, halfExtents),
        max: Vec3.add(center, halfExtents)
      });
      return this;
    }

    /**
     * Set a sphere collider on an entity.
     * @param {number} entity Filament entity ID
     * @param {number} radius Sphere radius
     * @param {Array} [center] Optional center offset [x,y,z], defaults to [0,0,0]
     * @returns {this}
     */
    setCollisionSphere(entity, radius, center) {
      center = center || [0, 0, 0];
      this._colliders.set(entity, {
        type: 'sphere',
        entity: entity,
        radius: radius,
        center: center
      });
      return this;
    }

    /**
     * Remove a collider from an entity.
     * @param {number} entity
     * @returns {this}
     */
    removeCollision(entity) {
      this._colliders.delete(entity);
      return this;
    }

    /**
     * Check if two entities' colliders overlap.
     * @param {number} entityA
     * @param {number} entityB
     * @returns {boolean}
     */
    checkCollision(entityA, entityB) {
      var a = this._colliders.get(entityA) || this._entityBoundsAsCollider(entityA);
      var b = this._colliders.get(entityB) || this._entityBoundsAsCollider(entityB);
      if (!a || !b) return false;

      if (a.type === 'box' && b.type === 'box') {
        return Collision.aabbOverlap(a.min, a.max, b.min, b.max);
      }
      if (a.type === 'sphere' && b.type === 'sphere') {
        return Collision.sphereOverlap(a.center, a.radius, b.center, b.radius);
      }
      if (a.type === 'sphere' && b.type === 'box') {
        return Collision.sphereAABBOverlap(a.center, a.radius, b.min, b.max);
      }
      if (a.type === 'box' && b.type === 'sphere') {
        return Collision.sphereAABBOverlap(b.center, b.radius, a.min, a.max);
      }
      return false;
    }

    /** Convert entity bounds to a collider-like object for checkCollision */
    _entityBoundsAsCollider(entity) {
      var bounds = this._entityBounds.get(entity);
      if (!bounds) return null;
      return {
        type: 'box',
        min: bounds.min,
        max: bounds.max,
        center: Vec3.scale(Vec3.add(bounds.min, bounds.max), 0.5)
      };
    }

    // =====================================================================
    // Gesture system — tap, double-tap, long-press, drag, pinch, rotate
    // =====================================================================

    /**
     * Register a tap callback. Fires when the user taps (clicks) on the canvas.
     * If the tap hits an entity, the entity and world-space hit point are provided.
     *
     * @param {Function} callback ({ entity, point, screenX, screenY }) => void
     * @returns {this}
     */
    onTap(callback) {
      this._gestureCallbacks.tap.push(callback);
      return this;
    }

    /**
     * Register a double-tap callback.
     * @param {Function} callback ({ entity, point, screenX, screenY }) => void
     * @returns {this}
     */
    onDoubleTap(callback) {
      this._gestureCallbacks.doubleTap.push(callback);
      return this;
    }

    /**
     * Register a long-press callback.
     * @param {Function} callback ({ entity, point, screenX, screenY }) => void
     * @param {number} [duration=500] Long press duration in ms
     * @returns {this}
     */
    onLongPress(callback, duration) {
      if (typeof duration === 'number' && duration > 0) {
        this._longPressDuration = duration;
      }
      this._gestureCallbacks.longPress.push(callback);
      return this;
    }

    /**
     * Register drag callbacks for a specific entity.
     * @param {number} entity Filament entity ID
     * @param {{ onStart: Function, onMove: Function, onEnd: Function }} callbacks
     *   onStart({ entity, point, screenX, screenY })
     *   onMove({ entity, point, delta, screenX, screenY })
     *   onEnd({ entity, point, screenX, screenY })
     * @returns {this}
     */
    onDrag(entity, callbacks) {
      this._gestureCallbacks.drag.set(entity, callbacks);
      return this;
    }

    /**
     * Register a pinch (two-finger zoom) callback.
     * @param {Function} callback ({ scale, centerX, centerY }) => void
     *   scale: ratio relative to pinch start (1 = no change, >1 = zoom in)
     * @returns {this}
     */
    onPinch(callback) {
      this._gestureCallbacks.pinch.push(callback);
      return this;
    }

    /**
     * Register a two-finger rotation callback.
     * @param {Function} callback ({ angle, centerX, centerY }) => void
     *   angle: rotation delta in radians since gesture start
     * @returns {this}
     */
    onRotate(callback) {
      this._gestureCallbacks.rotate.push(callback);
      return this;
    }

    /**
     * Enable automatic dragging of an entity in world space.
     * The entity will follow the pointer projected onto the constraint plane.
     *
     * @param {number} entity Filament entity ID
     * @param {{ axis: string, snap: number|null, bounds: { min: Array, max: Array }|null }} [options]
     *   axis: 'xz' (ground plane), 'xy' (vertical), or 'xyz' (free)
     *   snap: grid snap size (null = no snap)
     *   bounds: movement bounds (null = unlimited)
     * @returns {this}
     */
    enableEntityDrag(entity, options) {
      options = options || {};
      this._draggableEntities.set(entity, {
        axis: options.axis || 'xz',
        snap: options.snap || null,
        bounds: options.bounds || null
      });
      return this;
    }

    /**
     * Disable automatic dragging for an entity.
     * @param {number} entity
     * @returns {this}
     */
    disableEntityDrag(entity) {
      this._draggableEntities.delete(entity);
      if (this._activeDragEntity === entity) {
        this._activeDragEntity = null;
        this._activeDragPlane = null;
        this._activeDragLastWorld = null;
      }
      return this;
    }

    // =====================================================================
    // Selection system — selectable entities with visual highlight
    // =====================================================================

    /**
     * Mark an entity as selectable/unselectable.
     * @param {number} entity Filament entity ID
     * @param {boolean} enabled
     * @returns {this}
     */
    setSelectable(entity, enabled) {
      if (enabled) {
        this._selectableEntities.add(entity);
      } else {
        this._selectableEntities.delete(entity);
        if (this._selectedEntities.has(entity)) {
          this._selectedEntities.delete(entity);
          this._applySelectionHighlight(entity, false);
          this._fireSelectCallbacks(entity, false);
        }
      }
      return this;
    }

    /**
     * Register a selection change callback.
     * @param {Function} callback ({ entity, selected }) => void
     * @returns {this}
     */
    onSelect(callback) {
      this._gestureCallbacks.select.push(callback);
      return this;
    }

    /**
     * Get all currently selected entities.
     * @returns {Array<number>}
     */
    getSelectedEntities() {
      return Array.from(this._selectedEntities);
    }

    /**
     * Programmatically select an entity.
     * @param {number} entity
     * @param {boolean} [selected=true]
     * @returns {this}
     */
    selectEntity(entity, selected) {
      selected = selected !== false;
      if (!this._selectableEntities.has(entity)) return this;
      if (selected) {
        this._selectedEntities.add(entity);
      } else {
        this._selectedEntities.delete(entity);
      }
      this._applySelectionHighlight(entity, selected);
      this._fireSelectCallbacks(entity, selected);
      return this;
    }

    _fireSelectCallbacks(entity, selected) {
      var cbs = this._gestureCallbacks.select;
      for (var i = 0; i < cbs.length; i++) {
        try { cbs[i]({ entity: entity, selected: selected }); } catch (e) { console.error('SceneView select callback error:', e); }
      }
    }

    /**
     * Apply visual selection feedback via color tint using the material API.
     * Uses a subtle emissive tint to highlight selected entities.
     * Falls back gracefully if material access is unavailable.
     */
    _applySelectionHighlight(entity, selected) {
      try {
        var rm = this._engine.getRenderableManager();
        var inst = rm.getInstance(entity);
        if (!inst) return;

        // Try to modify the material's emissive to indicate selection
        var count = rm.getPrimitiveCount(inst);
        for (var p = 0; p < count; p++) {
          try {
            var mat = rm.getMaterialInstanceAt(inst, p);
            if (mat) {
              if (selected) {
                // Apply a blue-ish highlight tint via emissive
                mat.setColor3Parameter('emissive', Filament.RgbType.LINEAR, [0.15, 0.25, 0.5]);
              } else {
                // Remove highlight
                mat.setColor3Parameter('emissive', Filament.RgbType.LINEAR, [0, 0, 0]);
              }
            }
          } catch (e) { /* material param may not exist */ }
        }
      } catch (e) {
        // Filament material API may not be available — selection still
        // works logically, just without visual feedback
      }
    }

    // =====================================================================
    // Input handling — unified pointer events with gesture detection
    // =====================================================================

    _setupControls() {
      var canvas = this._canvas;
      var self = this;

      // Use pointer events for unified mouse + touch handling
      canvas.style.touchAction = 'none'; // Prevent browser gestures

      canvas.addEventListener('pointerdown', function(e) {
        canvas.setPointerCapture(e.pointerId);
        var rect = canvas.getBoundingClientRect();
        var sx = e.clientX - rect.left;
        var sy = e.clientY - rect.top;

        self._pointers.set(e.pointerId, {
          x: sx, y: sy,
          startX: sx, startY: sy,
          startTime: Date.now(),
          moved: false
        });

        // Two-pointer gesture start (pinch/rotate)
        if (self._pointers.size === 2) {
          var pts = Array.from(self._pointers.values());
          self._pinchStartDist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
          self._rotateStartAngle = Math.atan2(pts[1].y - pts[0].y, pts[1].x - pts[0].x);
          self._gestureConsumed = true;
          self._cancelLongPress();
          return;
        }

        // Single pointer — try entity interaction first
        self._gestureConsumed = false;

        var hit = self.hitTest(sx, sy);

        // Check if hit a draggable entity
        if (hit && self._draggableEntities.has(hit.entity)) {
          self._gestureConsumed = true;
          self._startEntityDrag(hit.entity, hit.point, sx, sy);
          return;
        }

        // Check if hit an entity with custom drag callbacks
        if (hit && self._gestureCallbacks.drag.has(hit.entity)) {
          self._gestureConsumed = true;
          self._activeDragEntity = hit.entity;
          self._activeDragLastWorld = hit.point;
          self._activeDragPlane = self._computeDragPlane(hit.point, 'xz');
          var dragCb = self._gestureCallbacks.drag.get(hit.entity);
          if (dragCb.onStart) {
            try { dragCb.onStart({ entity: hit.entity, point: hit.point, screenX: sx, screenY: sy }); } catch (err) { console.error(err); }
          }
          return;
        }

        // Start long-press timer
        self._startLongPress(sx, sy, hit);

        // Fall through to camera orbit
        if (!self._gestureConsumed) {
          self._isDragging = true;
          self._lastMouse = { x: e.clientX, y: e.clientY };
          self._autoRotate = false;
          self._velocityAngle = 0;
          self._velocityHeight = 0;
          if (self._autoRotateTimer) { clearTimeout(self._autoRotateTimer); self._autoRotateTimer = null; }
        }
      });

      canvas.addEventListener('pointermove', function(e) {
        var rect = canvas.getBoundingClientRect();
        var sx = e.clientX - rect.left;
        var sy = e.clientY - rect.top;

        var ptr = self._pointers.get(e.pointerId);
        if (ptr) {
          ptr.x = sx;
          ptr.y = sy;
          // Track if pointer has moved beyond tap threshold
          var distFromStart = Math.hypot(sx - ptr.startX, sy - ptr.startY);
          if (distFromStart > self._tapDistanceThreshold) {
            ptr.moved = true;
            self._cancelLongPress();
          }
        }

        // Two-pointer gestures (pinch / rotate)
        if (self._pointers.size === 2) {
          var pts = Array.from(self._pointers.values());
          var dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
          var angle = Math.atan2(pts[1].y - pts[0].y, pts[1].x - pts[0].x);
          var centerX = (pts[0].x + pts[1].x) / 2;
          var centerY = (pts[0].y + pts[1].y) / 2;

          // Pinch
          if (self._pinchStartDist > 0) {
            var scale = dist / self._pinchStartDist;
            var pinchCbs = self._gestureCallbacks.pinch;
            for (var pi = 0; pi < pinchCbs.length; pi++) {
              try { pinchCbs[pi]({ scale: scale, centerX: centerX, centerY: centerY }); } catch (err) { console.error(err); }
            }
            // Also drive camera zoom if no custom pinch handlers
            if (pinchCbs.length === 0) {
              self._orbitRadius /= (scale > 1 ? 1.01 : 0.99);
              self._orbitRadius = Math.max(0.5, Math.min(50, self._orbitRadius));
            }
          }

          // Rotate
          var angleDelta = angle - self._rotateStartAngle;
          var rotateCbs = self._gestureCallbacks.rotate;
          for (var ri = 0; ri < rotateCbs.length; ri++) {
            try { rotateCbs[ri]({ angle: angleDelta, centerX: centerX, centerY: centerY }); } catch (err) { console.error(err); }
          }
          return;
        }

        // Entity drag in progress
        if (self._activeDragEntity !== null && self._activeDragPlane) {
          self._updateEntityDrag(sx, sy);
          return;
        }

        // Camera orbit drag
        if (self._isDragging && !self._gestureConsumed) {
          var dx = (e.clientX - self._lastMouse.x) * 0.005;
          var dy = (e.clientY - self._lastMouse.y) * 0.01;
          self._velocityAngle = -dx;
          self._velocityHeight = dy;
          self._angle -= dx;
          self._orbitHeight += dy;
          self._lastMouse = { x: e.clientX, y: e.clientY };
        }
      });

      canvas.addEventListener('pointerup', function(e) {
        canvas.releasePointerCapture(e.pointerId);
        var ptr = self._pointers.get(e.pointerId);
        self._pointers.delete(e.pointerId);

        // End entity drag
        if (self._activeDragEntity !== null) {
          self._endEntityDrag(ptr ? ptr.x : 0, ptr ? ptr.y : 0);
        }

        // Detect gestures only for single-pointer releases
        if (ptr && !ptr.moved && self._pointers.size === 0) {
          var elapsed = Date.now() - ptr.startTime;
          var sx = ptr.startX;
          var sy = ptr.startY;

          self._cancelLongPress();

          // It was a tap (not a drag, not a long-press)
          if (elapsed < 500) {
            var hit = self.hitTest(sx, sy);

            // Double-tap detection
            var now = Date.now();
            var tapDist = Math.hypot(sx - self._lastTapPos.x, sy - self._lastTapPos.y);
            if (now - self._lastTapTime < self._doubleTapInterval && tapDist < self._tapDistanceThreshold * 2) {
              // Double tap
              var dtCbs = self._gestureCallbacks.doubleTap;
              for (var i = 0; i < dtCbs.length; i++) {
                try { dtCbs[i]({ entity: hit ? hit.entity : null, point: hit ? hit.point : null, screenX: sx, screenY: sy }); } catch (err) { console.error(err); }
              }
              self._lastTapTime = 0; // Reset to prevent triple-tap = double
            } else {
              // Single tap
              self._lastTapTime = now;
              self._lastTapPos = { x: sx, y: sy };

              // Fire tap callbacks
              var tapCbs = self._gestureCallbacks.tap;
              for (var j = 0; j < tapCbs.length; j++) {
                try { tapCbs[j]({ entity: hit ? hit.entity : null, point: hit ? hit.point : null, screenX: sx, screenY: sy }); } catch (err) { console.error(err); }
              }

              // Handle selection
              if (hit && self._selectableEntities.has(hit.entity)) {
                var wasSelected = self._selectedEntities.has(hit.entity);
                if (wasSelected) {
                  self._selectedEntities.delete(hit.entity);
                } else {
                  self._selectedEntities.add(hit.entity);
                }
                self._applySelectionHighlight(hit.entity, !wasSelected);
                self._fireSelectCallbacks(hit.entity, !wasSelected);
              }
            }
          }
        }

        // Camera orbit end
        if (self._pointers.size === 0) {
          self._isDragging = false;
          self._gestureConsumed = false;
          if (self._wantsAutoRotate) {
            self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
          }
        }
      });

      canvas.addEventListener('pointercancel', function(e) {
        self._pointers.delete(e.pointerId);
        if (self._activeDragEntity !== null) {
          self._endEntityDrag(0, 0);
        }
        self._cancelLongPress();
        if (self._pointers.size === 0) {
          self._isDragging = false;
          self._gestureConsumed = false;
          if (self._wantsAutoRotate) {
            self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
          }
        }
      });

      canvas.addEventListener('pointerleave', function(e) {
        self._pointers.delete(e.pointerId);
        self._cancelLongPress();
        if (self._pointers.size === 0) {
          self._isDragging = false;
          if (self._activeDragEntity !== null) {
            self._endEntityDrag(0, 0);
          }
          if (self._wantsAutoRotate) {
            self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
          }
        }
      });

      canvas.addEventListener('wheel', function(e) {
        e.preventDefault();
        self._orbitRadius *= (1 + e.deltaY * 0.001);
        self._orbitRadius = Math.max(0.5, Math.min(50, self._orbitRadius));
      }, { passive: false });
    }

    // =====================================================================
    // Long-press detection
    // =====================================================================

    _startLongPress(sx, sy, hit) {
      var self = this;
      this._cancelLongPress();
      if (this._gestureCallbacks.longPress.length === 0) return;
      this._longPressTimer = setTimeout(function() {
        self._longPressTimer = null;
        // Re-check hit at original position (pointer may not have moved)
        var actualHit = hit || self.hitTest(sx, sy);
        var cbs = self._gestureCallbacks.longPress;
        for (var i = 0; i < cbs.length; i++) {
          try { cbs[i]({ entity: actualHit ? actualHit.entity : null, point: actualHit ? actualHit.point : null, screenX: sx, screenY: sy }); } catch (err) { console.error(err); }
        }
        // Consume the gesture so it does not turn into a tap on release
        self._pointers.forEach(function(ptr) { ptr.moved = true; });
      }, this._longPressDuration);
    }

    _cancelLongPress() {
      if (this._longPressTimer) {
        clearTimeout(this._longPressTimer);
        this._longPressTimer = null;
      }
    }

    // =====================================================================
    // Entity dragging — world-space plane-constrained movement
    // =====================================================================

    /**
     * Compute the drag constraint plane for an axis mode.
     * Returns { normal: [x,y,z], d: number } where dot(normal, P) = d for points on the plane.
     */
    _computeDragPlane(worldPoint, axis) {
      var cam = this._getCameraMatrices();
      var viewDir = Vec3.normalize(Vec3.sub(this._orbitTarget, cam.eye));
      var normal;

      if (axis === 'xz') {
        // Horizontal ground plane (Y-up)
        normal = [0, 1, 0];
      } else if (axis === 'xy') {
        // Vertical plane facing camera
        normal = [0, 0, 1];
        // Use the camera's forward Z direction projected to XY
        if (Math.abs(viewDir[2]) > 0.1) {
          normal = Vec3.normalize([0, 0, viewDir[2] > 0 ? -1 : 1]);
        }
      } else {
        // 'xyz' — free movement on a plane facing the camera
        normal = Vec3.normalize(Vec3.negate(viewDir));
      }

      return { normal: normal, d: Vec3.dot(normal, worldPoint) };
    }

    _startEntityDrag(entity, worldPoint, sx, sy) {
      var dragOpts = this._draggableEntities.get(entity);
      var axis = dragOpts ? dragOpts.axis : 'xz';

      this._activeDragEntity = entity;
      this._activeDragLastWorld = worldPoint;
      this._activeDragPlane = this._computeDragPlane(worldPoint, axis);

      // Also fire custom drag callbacks if registered
      var dragCb = this._gestureCallbacks.drag.get(entity);
      if (dragCb && dragCb.onStart) {
        try { dragCb.onStart({ entity: entity, point: worldPoint, screenX: sx, screenY: sy }); } catch (err) { console.error(err); }
      }
    }

    _updateEntityDrag(sx, sy) {
      if (!this._activeDragEntity || !this._activeDragPlane) return;

      var ray = this.screenToRay(sx, sy);
      var plane = this._activeDragPlane;
      var hit = Collision.rayPlane(ray.origin, ray.direction, plane.normal, plane.d);
      if (!hit) return;

      var newWorld = hit.point;
      var entity = this._activeDragEntity;
      var dragOpts = this._draggableEntities.get(entity);

      // Apply snapping
      if (dragOpts && dragOpts.snap) {
        var snap = dragOpts.snap;
        newWorld = [
          Math.round(newWorld[0] / snap) * snap,
          Math.round(newWorld[1] / snap) * snap,
          Math.round(newWorld[2] / snap) * snap
        ];
      }

      // Apply bounds
      if (dragOpts && dragOpts.bounds) {
        var b = dragOpts.bounds;
        newWorld = Vec3.max(b.min, Vec3.min(b.max, newWorld));
      }

      var delta = Vec3.sub(newWorld, this._activeDragLastWorld);
      this._activeDragLastWorld = newWorld;

      // Move the entity using Filament's TransformManager
      try {
        var tm = this._engine.getTransformManager();
        var inst = tm.getInstance(entity);
        if (inst) {
          var transform = tm.getTransform(inst);
          // The transform is a 4x4 column-major matrix — update translation
          transform[12] += delta[0];
          transform[13] += delta[1];
          transform[14] += delta[2];
          tm.setTransform(inst, transform);
        }
      } catch (e) {
        // TransformManager may not be available for all entities
      }

      // Update collider position if one exists
      var collider = this._colliders.get(entity);
      if (collider) {
        collider.center = Vec3.add(collider.center, delta);
        if (collider.type === 'box') {
          collider.min = Vec3.sub(collider.center, collider.halfExtents);
          collider.max = Vec3.add(collider.center, collider.halfExtents);
        }
      }

      // Update cached bounds
      var bounds = this._entityBounds.get(entity);
      if (bounds) {
        bounds.min = Vec3.add(bounds.min, delta);
        bounds.max = Vec3.add(bounds.max, delta);
      }

      // Fire custom drag callbacks
      var dragCb = this._gestureCallbacks.drag.get(entity);
      if (dragCb && dragCb.onMove) {
        try { dragCb.onMove({ entity: entity, point: newWorld, delta: delta, screenX: sx, screenY: sy }); } catch (err) { console.error(err); }
      }
    }

    _endEntityDrag(sx, sy) {
      var entity = this._activeDragEntity;
      if (entity === null) return;

      var dragCb = this._gestureCallbacks.drag.get(entity);
      if (dragCb && dragCb.onEnd) {
        try { dragCb.onEnd({ entity: entity, point: this._activeDragLastWorld, screenX: sx, screenY: sy }); } catch (err) { console.error(err); }
      }

      this._activeDragEntity = null;
      this._activeDragPlane = null;
      this._activeDragLastWorld = null;
    }

    // =====================================================================
    // Resize handling
    // =====================================================================

    _setupResizeObserver() {
      var self = this;
      this._resizeObserver = new ResizeObserver(function() {
        var canvas = self._canvas;
        var dpr = Math.min(devicePixelRatio, 2); // Cap at 2x for performance
        canvas.width = canvas.clientWidth * dpr;
        canvas.height = canvas.clientHeight * dpr;
        self._view.setViewport([0, 0, canvas.width, canvas.height]);
        self._camera.setProjectionFov(
          self._fov || 45, canvas.width / canvas.height, 0.1, 1000,
          Filament.Camera$Fov.VERTICAL
        );
      });
      this._resizeObserver.observe(this._canvas);
    }

    // =====================================================================
    // Render loop
    // =====================================================================

    _startRenderLoop() {
      var self = this;
      function render() {
        if (!self._running) return;

        // Auto-rotate: 30deg/sec / 60fps (matches model-viewer)
        if (self._autoRotate) self._angle += 0.00873;

        // Inertia damping after drag release
        if (!self._isDragging) {
          self._angle += self._velocityAngle;
          self._orbitHeight += self._velocityHeight;
          self._velocityAngle *= self._dampingFactor;
          self._velocityHeight *= self._dampingFactor;
          if (Math.abs(self._velocityAngle) < 0.00005) self._velocityAngle = 0;
          if (Math.abs(self._velocityHeight) < 0.00005) self._velocityHeight = 0;
        }

        var t = self._orbitTarget;
        var r = self._orbitRadius;
        var h = self._orbitHeight;
        self._camera.lookAt(
          [t[0] + Math.sin(self._angle) * r, h, t[2] + Math.cos(self._angle) * r],
          t,
          [0, 1, 0]
        );

        self._engine.execute();
        try {
          if (self._renderer.beginFrame(self._swapChain)) {
            self._renderer.renderView(self._view);
            self._renderer.endFrame();
          }
        } catch (e) {
          // Filament 1.70 may need different render call
          console.error('SceneView render error:', e.message);
          self._running = false;
        }
        requestAnimationFrame(render);
      }
      render();
    }
  }

  // Singleton guard — prevent multiple engine creations on same canvas
  var _activeCanvases = new Set();

  /**
   * Set up Filament engine, scene, lights on a canvas.
   */
  function _createEngine(canvasOrId, options) {
    options = options || {};

    var canvas = typeof canvasOrId === 'string'
      ? document.getElementById(canvasOrId)
      : canvasOrId;
    if (!canvas) throw new Error('Canvas not found: ' + canvasOrId);

    // Prevent double initialization on the same canvas
    if (_activeCanvases.has(canvas)) {
      console.warn('SceneView: Canvas already initialized, skipping');
      return null;
    }
    _activeCanvases.add(canvas);

    var dpr = Math.min(devicePixelRatio, 2);
    // Ensure canvas has actual layout dimensions (not default 300x150)
    var cssW = canvas.clientWidth || canvas.offsetWidth || 500;
    var cssH = canvas.clientHeight || canvas.offsetHeight || 500;
    canvas.width = cssW * dpr;
    canvas.height = cssH * dpr;

    var engine = Filament.Engine.create(canvas);
    var scene = engine.createScene();
    var renderer = engine.createRenderer();
    var cameraEntity = Filament.EntityManager.get().create();
    var camera = engine.createCamera(cameraEntity);
    var view = engine.createView();
    var swapChain = engine.createSwapChain();

    view.setCamera(camera);
    view.setScene(scene);
    view.setViewport([0, 0, canvas.width, canvas.height]);

    var bg = options.backgroundColor || [0.05, 0.06, 0.1, 1.0];
    renderer.setClearOptions({ clearColor: bg, clear: true });

    var fov = options.fov || 45;
    camera.setProjectionFov(fov, canvas.width / canvas.height, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
    camera.lookAt([0, 1, 5], [0, 0, 0], [0, 1, 0]);

    // --- Post-processing quality ---
    try {
      view.setAmbientOcclusionOptions({
        enabled: true, radius: 0.3, bias: 0.0005, intensity: 1.0, quality: 1
      });
    } catch (e) { /* skip */ }

    // --- 3-point studio lighting ---
    // Sun/key light — warm, strong
    var sun = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.SUN)
      .color([0.98, 0.92, 0.89])
      .intensity(options.lightIntensity || 110000)
      .direction([0.6, -1.0, -0.8])
      .sunAngularRadius(1.9)
      .sunHaloSize(10.0)
      .sunHaloFalloff(80.0)
      .build(engine, sun);
    scene.addEntity(sun);

    // Fill light — cool, softer
    var fill = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
      .color([0.7, 0.75, 0.9])
      .intensity(60000)
      .direction([-0.5, 0.5, 1.0])
      .build(engine, fill);
    scene.addEntity(fill);

    // Back/rim light — edge highlight
    var back = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
      .color([0.5, 0.6, 0.9])
      .intensity(50000)
      .direction([0, 0.3, 1.0])
      .build(engine, back);
    scene.addEntity(back);

    // --- IBL: load real KTX if available, fallback to synthetic SH ---
    var iblUrl = options.iblUrl || 'environments/neutral_ibl.ktx';
    fetch(iblUrl)
      .then(function(r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.arrayBuffer().then(function(ab) { return new Uint8Array(ab); });
      })
      .then(function(buffer) {
        try {
          var ibl = engine.createIblFromKtx1(buffer);
          ibl.setIntensity(options.iblIntensity || 40000);
          scene.setIndirectLight(ibl);
          // Create skybox from IBL reflection cubemap if skybox enabled
          if (options.skybox !== false) {
            try {
              var reflections = ibl.getReflectionsTexture();
              if (reflections) {
                var skybox = Filament.Skybox.Builder()
                  .environment(reflections)
                  .build(engine);
                scene.setSkybox(skybox);
                console.log('SceneView: Skybox created from IBL cubemap');
              }
            } catch (skyErr) {
              // Skybox not supported in this build — that's OK
              console.log('SceneView: Skybox not available (IBL-only mode)');
            }
          }
          console.log('SceneView: KTX IBL loaded (' + Math.round(buffer.length / 1024) + 'KB)');
        } catch (e) {
          console.warn('SceneView: createIblFromKtx1 failed, using SH fallback', e);
          _applySyntheticIBL(engine, scene);
        }
      })
      .catch(function() {
        _applySyntheticIBL(engine, scene);
      });

    var loader = engine.createAssetLoader();
    var instance = new SceneViewInstance(canvas, engine, scene, renderer, view, swapChain, camera, cameraEntity, loader);
    instance._fov = fov;

    if (options.autoRotate === false) instance.setAutoRotate(false);

    return instance;
  }

  /** Fallback IBL from spherical harmonics when KTX not available */
  function _applySyntheticIBL(engine, scene) {
    try {
      var ibl = Filament.IndirectLight.Builder()
        .irradiance(3, [
           0.65,  0.65,  0.70,
           0.10,  0.10,  0.12,
           0.15,  0.15,  0.18,
          -0.02, -0.02, -0.01,
           0.04,  0.04,  0.05,
           0.08,  0.08,  0.10,
           0.01,  0.01,  0.01,
          -0.02, -0.02, -0.02,
           0.03,  0.03,  0.03
        ])
        .intensity(35000)
        .build(engine);
      scene.setIndirectLight(ibl);
      console.log('SceneView: Using synthetic SH IBL');
    } catch (e) { /* skip */ }
  }

  function create(canvasOrId, options) {
    return _ensureFilament().then(function() {
      return new Promise(function(resolve, reject) {
        if (typeof Filament.Engine !== 'undefined') {
          try {
            var instance = _createEngine(canvasOrId, options);
            if (instance) resolve(instance);
            else reject(new Error('SceneView: Canvas already initialized'));
          } catch (e) { reject(e); }
          return;
        }
        Filament.init([], function() {
          try {
            var instance = _createEngine(canvasOrId, options);
            if (instance) resolve(instance);
            else reject(new Error('SceneView: Canvas already initialized'));
          } catch (e) { reject(e); }
        });
      });
    });
  }

  function modelViewer(canvasOrId, modelUrl, options) {
    return create(canvasOrId, options).then(function(instance) {
      return instance.loadModel(modelUrl);
    });
  }

  // Expose internals for advanced usage and testing
  global.SceneView = {
    version: '1.4.0',
    create: create,
    modelViewer: modelViewer,
    // Math utilities (for advanced users)
    Vec3: Vec3,
    Mat4: Mat4,
    Collision: Collision
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
