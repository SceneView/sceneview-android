/**
 * SceneView Web — Simple 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * Powered by Filament.js (Google's PBR renderer, WASM).
 * https://sceneview.github.io
 *
 * @version 1.0.0
 * @license MIT
 */
(function(global) {
  'use strict';

  const FILAMENT_CDN = 'https://cdn.jsdelivr.net/npm/filament@1.52.3/filament.js';
  let filamentLoaded = false;
  let filamentLoading = false;
  let filamentCallbacks = [];

  // Load Filament.js from CDN if not already loaded
  function ensureFilament(callback) {
    if (filamentLoaded && global.Filament) {
      callback();
      return;
    }
    filamentCallbacks.push(callback);
    if (filamentLoading) return;
    filamentLoading = true;

    const script = document.createElement('script');
    script.src = FILAMENT_CDN;
    script.onload = function() {
      filamentLoaded = true;
      const cbs = filamentCallbacks.splice(0);
      cbs.forEach(cb => cb());
    };
    script.onerror = function() {
      console.error('SceneView: Failed to load Filament.js from CDN');
    };
    document.head.appendChild(script);
  }

  /**
   * SceneView instance — wraps Filament engine, scene, camera, renderer.
   */
  class SceneViewInstance {
    constructor(canvas, engine, scene, renderer, view, swapChain, camera, cameraEntity) {
      this._canvas = canvas;
      this._engine = engine;
      this._scene = scene;
      this._renderer = renderer;
      this._view = view;
      this._swapChain = swapChain;
      this._camera = camera;
      this._cameraEntity = cameraEntity;
      this._asset = null;
      this._angle = 0;
      this._autoRotate = true;
      this._orbitRadius = 3.5;
      this._orbitHeight = 1.0;
      this._orbitTarget = [0, 0, 0];
      this._running = true;
      this._isDragging = false;
      this._lastMouse = { x: 0, y: 0 };
      this._setupControls();
      this._startRenderLoop();
    }

    /** Load a glTF/GLB model from URL */
    loadModel(url) {
      const self = this;
      return new Promise(function(resolve, reject) {
        Filament.init([url], function() {
          try {
            const loader = self._engine.createAssetLoader();
            const data = Filament.assets[url];
            if (!data) { reject(new Error('Failed to fetch model')); return; }

            const asset = loader.createAsset(data);
            if (!asset) { reject(new Error('Failed to parse model')); return; }

            // Remove previous model
            if (self._asset) {
              const oldEntities = self._asset.getRenderableEntities();
              oldEntities.forEach(e => self._scene.remove(e));
              self._scene.remove(self._asset.getRoot());
            }

            // Load textures/materials
            asset.loadResources();

            // Add to scene
            self._scene.addEntity(asset.getRoot());
            self._scene.addEntities(asset.getRenderableEntities());
            self._asset = asset;

            // Auto-frame the model
            const bbox = asset.getBoundingBox();
            const cx = (bbox.min[0] + bbox.max[0]) / 2;
            const cy = (bbox.min[1] + bbox.max[1]) / 2;
            const cz = (bbox.min[2] + bbox.max[2]) / 2;
            const sx = bbox.max[0] - bbox.min[0];
            const sy = bbox.max[1] - bbox.min[1];
            const sz = bbox.max[2] - bbox.min[2];
            const maxDim = Math.max(sx, sy, sz);
            self._orbitTarget = [cx, cy, cz];
            self._orbitRadius = maxDim * 2.5;
            self._orbitHeight = cy;

            resolve(self);
          } catch (e) {
            reject(e);
          }
        });
      });
    }

    /** Enable/disable auto-rotation */
    setAutoRotate(enabled) {
      this._autoRotate = enabled;
      return this;
    }

    /** Set camera distance */
    setCameraDistance(distance) {
      this._orbitRadius = distance;
      return this;
    }

    /** Set background color [r, g, b, a] (0-1) */
    setBackgroundColor(r, g, b, a) {
      this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true });
      return this;
    }

    /** Dispose all resources */
    dispose() {
      this._running = false;
      Filament.Engine.destroy(this._engine);
    }

    // --- Private ---

    _setupControls() {
      const canvas = this._canvas;
      const self = this;

      // Mouse orbit
      canvas.addEventListener('mousedown', function(e) {
        self._isDragging = true;
        self._lastMouse = { x: e.clientX, y: e.clientY };
        self._autoRotate = false;
      });
      canvas.addEventListener('mousemove', function(e) {
        if (!self._isDragging) return;
        const dx = e.clientX - self._lastMouse.x;
        const dy = e.clientY - self._lastMouse.y;
        self._angle += dx * 0.005;
        self._orbitHeight += dy * 0.01;
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
        const dx = e.touches[0].clientX - self._lastMouse.x;
        const dy = e.touches[0].clientY - self._lastMouse.y;
        self._angle += dx * 0.005;
        self._orbitHeight += dy * 0.01;
        self._lastMouse = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      }, { passive: false });
      canvas.addEventListener('touchend', function() { self._isDragging = false; });

      // Resize
      const ro = new ResizeObserver(function() {
        canvas.width = canvas.clientWidth * devicePixelRatio;
        canvas.height = canvas.clientHeight * devicePixelRatio;
        self._view.setViewport([0, 0, canvas.width, canvas.height]);
        self._camera.setProjectionFov(45, canvas.width / canvas.height, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
      });
      ro.observe(canvas);
    }

    _startRenderLoop() {
      const self = this;
      function render() {
        if (!self._running) return;

        if (self._autoRotate) {
          self._angle += 0.006;
        }

        const t = self._orbitTarget;
        const r = self._orbitRadius;
        const h = self._orbitHeight;
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
   * Create a SceneView instance on a canvas element.
   * @param {string|HTMLCanvasElement} canvasOrId - Canvas element or its ID
   * @param {Object} [options] - Configuration options
   * @returns {Promise<SceneViewInstance>}
   */
  function create(canvasOrId, options) {
    options = options || {};
    return new Promise(function(resolve, reject) {
      ensureFilament(function() {
        Filament.init([], function() {
          try {
            const canvas = typeof canvasOrId === 'string'
              ? document.getElementById(canvasOrId)
              : canvasOrId;

            if (!canvas) { reject(new Error('Canvas not found: ' + canvasOrId)); return; }

            canvas.width = canvas.clientWidth * devicePixelRatio;
            canvas.height = canvas.clientHeight * devicePixelRatio;

            const engine = Filament.Engine.create(canvas);
            const scene = engine.createScene();
            const renderer = engine.createRenderer();
            const cameraEntity = Filament.EntityManager.get().create();
            const camera = engine.createCamera(cameraEntity);
            const view = engine.createView();
            const swapChain = engine.createSwapChain();

            view.setCamera(camera);
            view.setScene(scene);
            view.setViewport([0, 0, canvas.width, canvas.height]);

            const bg = options.backgroundColor || [0.05, 0.06, 0.1, 1.0];
            renderer.setClearOptions({ clearColor: bg, clear: true });

            const aspect = canvas.width / canvas.height;
            camera.setProjectionFov(options.fov || 45, aspect, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
            camera.lookAt([0, 1, 5], [0, 0, 0], [0, 1, 0]);

            // Default sunlight
            const sun = Filament.EntityManager.get().create();
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
            const fill = Filament.EntityManager.get().create();
            Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
              .color([0.6, 0.65, 0.8])
              .intensity(30000)
              .direction([-0.5, 0.5, 1.0])
              .build(engine, fill);
            scene.addEntity(fill);

            const instance = new SceneViewInstance(canvas, engine, scene, renderer, view, swapChain, camera, cameraEntity);

            if (options.autoRotate === false) {
              instance.setAutoRotate(false);
            }

            resolve(instance);
          } catch (e) {
            reject(e);
          }
        });
      });
    });
  }

  /**
   * One-liner: create viewer and load a model.
   * @param {string|HTMLCanvasElement} canvasOrId
   * @param {string} modelUrl - URL to .glb/.gltf model
   * @param {Object} [options]
   * @returns {Promise<SceneViewInstance>}
   */
  function modelViewer(canvasOrId, modelUrl, options) {
    return create(canvasOrId, options).then(function(viewer) {
      return viewer.loadModel(modelUrl);
    });
  }

  // Public API
  global.SceneView = {
    version: '1.0.0',
    create: create,
    modelViewer: modelViewer
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
