/**
 * SceneView Web — Simple 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * Powered by Filament.js v1.70.1 (Google's PBR renderer, WASM).
 * https://sceneview.github.io
 *
 * @version 1.3.0
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

  /**
   * SceneView instance — wraps Filament engine, scene, camera, renderer.
   */
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
      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

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
          this._asset.getRenderableEntities().forEach(function(e) { this._scene.remove(e); }.bind(this));
          this._scene.remove(this._asset.getRoot());
        } catch (e) { /* ignore cleanup errors */ }
        this._asset = null;
      }

      var data = Filament.assets[url];
      if (!data) throw new Error('Failed to fetch model: ' + url);

      var asset = this._loader.createAsset(data);
      if (!asset) throw new Error('Failed to parse model: ' + url);

      asset.loadResources();
      this._scene.addEntity(asset.getRoot());
      this._scene.addEntities(asset.getRenderableEntities());
      this._asset = asset;

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

    setAutoRotate(enabled) { this._autoRotate = enabled; this._wantsAutoRotate = enabled; return this; }
    setCameraDistance(d) { this._orbitRadius = d; return this; }

    setBackgroundColor(r, g, b, a) {
      this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true });
      return this;
    }

    dispose() {
      this._running = false;
      if (this._resizeObserver) this._resizeObserver.disconnect();
      try { Filament.Engine.destroy(this._engine); } catch (e) { /* already destroyed */ }
    }

    _setupControls() {
      var canvas = this._canvas;
      var self = this;

      canvas.addEventListener('mousedown', function(e) {
        self._isDragging = true;
        self._lastMouse = { x: e.clientX, y: e.clientY };
        self._autoRotate = false;
        self._velocityAngle = 0;
        self._velocityHeight = 0;
        if (self._autoRotateTimer) { clearTimeout(self._autoRotateTimer); self._autoRotateTimer = null; }
      });
      canvas.addEventListener('mousemove', function(e) {
        if (!self._isDragging) return;
        var dx = (e.clientX - self._lastMouse.x) * 0.005;
        var dy = (e.clientY - self._lastMouse.y) * 0.01;
        self._velocityAngle = -dx;
        self._velocityHeight = dy;
        self._angle -= dx;
        self._orbitHeight += dy;
        self._lastMouse = { x: e.clientX, y: e.clientY };
      });
      canvas.addEventListener('mouseup', function() {
        self._isDragging = false;
        // Resume auto-rotate after 3s idle (like model-viewer)
        if (self._wantsAutoRotate) {
          self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
        }
      });
      canvas.addEventListener('mouseleave', function() {
        self._isDragging = false;
        if (self._wantsAutoRotate) {
          self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
        }
      });

      canvas.addEventListener('wheel', function(e) {
        e.preventDefault();
        self._orbitRadius *= (1 + e.deltaY * 0.001);
        self._orbitRadius = Math.max(0.5, Math.min(50, self._orbitRadius));
      }, { passive: false });

      canvas.addEventListener('touchstart', function(e) {
        if (e.touches.length === 1) {
          self._isDragging = true;
          self._lastMouse = { x: e.touches[0].clientX, y: e.touches[0].clientY };
          self._autoRotate = false;
          self._velocityAngle = 0;
          self._velocityHeight = 0;
          if (self._autoRotateTimer) { clearTimeout(self._autoRotateTimer); self._autoRotateTimer = null; }
        }
      });
      canvas.addEventListener('touchmove', function(e) {
        if (!self._isDragging || e.touches.length !== 1) return;
        e.preventDefault();
        var dx = (e.touches[0].clientX - self._lastMouse.x) * 0.005;
        var dy = (e.touches[0].clientY - self._lastMouse.y) * 0.01;
        self._velocityAngle = -dx;
        self._velocityHeight = dy;
        self._angle -= dx;
        self._orbitHeight += dy;
        self._lastMouse = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      }, { passive: false });
      canvas.addEventListener('touchend', function() {
        self._isDragging = false;
        if (self._wantsAutoRotate) {
          self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
        }
      });
    }

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

    _startRenderLoop() {
      var self = this;
      function render() {
        if (!self._running) return;

        // Auto-rotate: 30°/sec ÷ 60fps (matches model-viewer)
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

  global.SceneView = {
    version: '1.3.0',
    create: create,
    modelViewer: modelViewer
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
