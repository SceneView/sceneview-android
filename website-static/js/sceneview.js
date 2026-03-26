/**
 * SceneView Web — Simple 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * No prerequisites — sceneview.js loads Filament.js CDN automatically.
 * Just include one script:
 *   <script src="js/sceneview.js"></script>
 *
 * Powered by Filament.js (Google's PBR renderer, WASM).
 * https://sceneview.github.io
 *
 * @version 1.1.0
 * @license MIT
 */
(function(global) {
  'use strict';

  var FILAMENT_CDN = 'https://cdn.jsdelivr.net/npm/filament@1.52.3/filament.js';

  /**
   * Load Filament.js CDN dynamically if not already present.
   * Returns a Promise that resolves when the Filament global is available.
   */
  function _ensureFilament() {
    return new Promise(function(resolve, reject) {
      // Already loaded
      if (typeof Filament !== 'undefined') {
        resolve();
        return;
      }
      // Check if script tag already exists but hasn't finished loading
      var existing = document.querySelector('script[src*="filament"]');
      if (existing) {
        existing.addEventListener('load', function() { resolve(); });
        existing.addEventListener('error', function() { reject(new Error('SceneView: Failed to load Filament.js from CDN')); });
        return;
      }
      // Inject script tag
      var script = document.createElement('script');
      script.src = FILAMENT_CDN;
      script.onload = function() { resolve(); };
      script.onerror = function() { reject(new Error('SceneView: Failed to load Filament.js from CDN (' + FILAMENT_CDN + ')')); };
      document.head.appendChild(script);
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
      this._angle = 0;
      this._autoRotate = true;
      this._orbitRadius = 3.5;
      this._orbitHeight = 0.8;
      this._orbitTarget = [0, 0, 0];
      this._running = true;
      this._isDragging = false;
      this._lastMouse = { x: 0, y: 0 };
      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

    /** Load a glTF/GLB model from URL */
    loadModel(url) {
      var self = this;
      return new Promise(function(resolve, reject) {
        // If already fetched, show immediately
        if (Filament.assets && Filament.assets[url]) {
          try {
            self._showModel(url);
            resolve(self);
          } catch (e) {
            reject(e);
          }
          return;
        }
        // Fetch via Filament.init with asset — this always fires the callback
        // because it needs to fetch the asset even if WASM is already loaded
        Filament.init([url], function() {
          try {
            self._showModel(url);
            resolve(self);
          } catch (e) {
            reject(e);
          }
        });
      });
    }

    _showModel(url) {
      // Remove previous model
      if (this._asset) {
        this._asset.getRenderableEntities().forEach(function(e) { this._scene.remove(e); }.bind(this));
        this._scene.remove(this._asset.getRoot());
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

      // Auto-frame: try getBoundingBox, fall back to defaults
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
          this._orbitRadius = maxDim * 2.5;
          this._orbitHeight = cy;
        }
      } catch (e) {
        // getBoundingBox not available on all assets, use defaults
      }
    }

    /** Enable/disable auto-rotation */
    setAutoRotate(enabled) {
      this._autoRotate = enabled;
      return this;
    }

    /** Set camera orbit distance */
    setCameraDistance(distance) {
      this._orbitRadius = distance;
      return this;
    }

    /** Set background color [r, g, b, a] (0-1 range) */
    setBackgroundColor(r, g, b, a) {
      this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true });
      return this;
    }

    /** Dispose all resources */
    dispose() {
      this._running = false;
      if (this._resizeObserver) {
        this._resizeObserver.disconnect();
      }
      Filament.Engine.destroy(this._engine);
    }

    // --- Private ---

    _setupControls() {
      var canvas = this._canvas;
      var self = this;

      // Mouse orbit
      canvas.addEventListener('mousedown', function(e) {
        self._isDragging = true;
        self._lastMouse = { x: e.clientX, y: e.clientY };
        self._autoRotate = false;
      });
      canvas.addEventListener('mousemove', function(e) {
        if (!self._isDragging) return;
        self._angle -= (e.clientX - self._lastMouse.x) * 0.005;
        self._orbitHeight += (e.clientY - self._lastMouse.y) * 0.01;
        self._lastMouse = { x: e.clientX, y: e.clientY };
      });
      canvas.addEventListener('mouseup', function() { self._isDragging = false; });
      canvas.addEventListener('mouseleave', function() { self._isDragging = false; });

      // Scroll zoom
      canvas.addEventListener('wheel', function(e) {
        e.preventDefault();
        self._orbitRadius *= (1 + e.deltaY * 0.001);
        self._orbitRadius = Math.max(0.5, Math.min(50, self._orbitRadius));
      }, { passive: false });

      // Touch orbit
      canvas.addEventListener('touchstart', function(e) {
        if (e.touches.length === 1) {
          self._isDragging = true;
          self._lastMouse = { x: e.touches[0].clientX, y: e.touches[0].clientY };
          self._autoRotate = false;
        }
      });
      canvas.addEventListener('touchmove', function(e) {
        if (!self._isDragging || e.touches.length !== 1) return;
        e.preventDefault();
        self._angle -= (e.touches[0].clientX - self._lastMouse.x) * 0.005;
        self._orbitHeight += (e.touches[0].clientY - self._lastMouse.y) * 0.01;
        self._lastMouse = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      }, { passive: false });
      canvas.addEventListener('touchend', function() { self._isDragging = false; });
    }

    _setupResizeObserver() {
      var self = this;
      this._resizeObserver = new ResizeObserver(function() {
        var canvas = self._canvas;
        canvas.width = canvas.clientWidth * devicePixelRatio;
        canvas.height = canvas.clientHeight * devicePixelRatio;
        self._view.setViewport([0, 0, canvas.width, canvas.height]);
        self._camera.setProjectionFov(45, canvas.width / canvas.height, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
      });
      this._resizeObserver.observe(this._canvas);
    }

    _startRenderLoop() {
      var self = this;
      function render() {
        if (!self._running) return;
        if (self._autoRotate) self._angle += 0.006;

        var t = self._orbitTarget;
        var r = self._orbitRadius;
        var h = self._orbitHeight;
        self._camera.lookAt(
          [t[0] + Math.sin(self._angle) * r, h, t[2] + Math.cos(self._angle) * r],
          t,
          [0, 1, 0]
        );

        if (self._renderer.beginFrame(self._swapChain)) {
          self._renderer.render(self._swapChain, self._view);
          self._renderer.endFrame();
        }
        self._engine.execute();
        requestAnimationFrame(render);
      }
      render();
    }
  }

  /**
   * Internal: set up Filament engine, scene, lights on a canvas.
   * Called inside a Filament.init() callback where WASM is guaranteed ready.
   */
  function _createEngine(canvasOrId, options) {
    options = options || {};

    var canvas = typeof canvasOrId === 'string'
      ? document.getElementById(canvasOrId)
      : canvasOrId;

    if (!canvas) throw new Error('Canvas not found: ' + canvasOrId);

    canvas.width = canvas.clientWidth * devicePixelRatio;
    canvas.height = canvas.clientHeight * devicePixelRatio;

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

    var aspect = canvas.width / canvas.height;
    camera.setProjectionFov(options.fov || 45, aspect, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
    camera.lookAt([0, 1, 5], [0, 0, 0], [0, 1, 0]);

    // Default sunlight
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

    // Fill light
    var fill = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
      .color([0.6, 0.65, 0.8])
      .intensity(30000)
      .direction([-0.5, 0.5, 1.0])
      .build(engine, fill);
    scene.addEntity(fill);

    // Asset loader (reused across model loads)
    var loader = engine.createAssetLoader();

    var instance = new SceneViewInstance(canvas, engine, scene, renderer, view, swapChain, camera, cameraEntity, loader);

    if (options.autoRotate === false) {
      instance.setAutoRotate(false);
    }

    return instance;
  }

  /**
   * Create an empty SceneView on a canvas.
   * Filament.js is loaded automatically from CDN if not already present.
   *
   * @param {string|HTMLCanvasElement} canvasOrId - Canvas element or its ID
   * @param {Object} [options] - Configuration options
   * @returns {Promise<SceneViewInstance>}
   */
  function create(canvasOrId, options) {
    return _ensureFilament().then(function() {
      return new Promise(function(resolve, reject) {
        // If WASM is already initialized (Engine exists), skip Filament.init
        if (typeof Filament.Engine !== 'undefined') {
          try {
            resolve(_createEngine(canvasOrId, options));
          } catch (e) {
            reject(e);
          }
          return;
        }
        // First time: initialize WASM
        Filament.init([], function() {
          try {
            resolve(_createEngine(canvasOrId, options));
          } catch (e) {
            reject(e);
          }
        });
      });
    });
  }

  /**
   * One-liner: create viewer and load a model.
   * Filament.js is loaded automatically from CDN if not already present.
   *
   * @param {string|HTMLCanvasElement} canvasOrId
   * @param {string} modelUrl - URL to .glb/.gltf model
   * @param {Object} [options]
   * @returns {Promise<SceneViewInstance>}
   */
  function modelViewer(canvasOrId, modelUrl, options) {
    return _ensureFilament().then(function() {
      return new Promise(function(resolve, reject) {
        // Always use Filament.init with the model URL in the assets array.
        // This works whether WASM is already loaded or not, because Filament
        // needs to fetch the model asset and will call back when done.
        Filament.init([modelUrl], function() {
          try {
            var instance = _createEngine(canvasOrId, options);
            instance._showModel(modelUrl);
            resolve(instance);
          } catch (e) {
            reject(e);
          }
        });
      });
    });
  }

  // Public API
  global.SceneView = {
    version: '1.1.0',
    create: create,
    modelViewer: modelViewer
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
