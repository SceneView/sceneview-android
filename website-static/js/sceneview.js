/**
 * SceneView Web — Simple 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * Powered by Filament.js v1.70.1 (Google's PBR renderer, WASM).
 * https://sceneview.github.io
 *
 * @version 1.5.0
 * @license MIT
 */
(function(global) {
  'use strict';

  function _ensureFilament() {
    return new Promise(function(resolve, reject) {
      if (typeof Filament !== 'undefined') { resolve(); return; }
      var attempts = 0;
      var check = setInterval(function() {
        if (typeof Filament !== 'undefined') { clearInterval(check); resolve(); }
        if (++attempts > 100) { clearInterval(check); reject(new Error('SceneView: Filament.js not loaded')); }
      }, 50);
    });
  }

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
      this._angle = 0.785;
      this._autoRotate = true;
      this._orbitRadius = 3.5;
      this._orbitHeight = 0.8;
      this._orbitTarget = [0, 0, 0];
      this._running = true;
      this._isDragging = false;
      this._lastMouse = { x: 0, y: 0 };
      this._velocityAngle = 0;
      this._velocityHeight = 0;
      this._dampingFactor = 0.95;
      this._wantsAutoRotate = true;
      this._autoRotateTimer = null;
      this._userLights = [];
      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

    // ── Model loading ──

    loadModel(url) {
      var self = this;
      return new Promise(function(resolve, reject) {
        fetch(url)
          .then(function(resp) { return resp.arrayBuffer(); })
          .then(function(buffer) {
            Filament.assets = Filament.assets || {};
            Filament.assets[url] = new Uint8Array(buffer);
            try { self._showModel(url); resolve(self); } catch (e) { reject(e); }
          })
          .catch(reject);
      });
    }

    _showModel(url) {
      if (this._asset) {
        try {
          this._asset.getRenderableEntities().forEach(function(e) { this._scene.remove(e); }.bind(this));
          this._scene.remove(this._asset.getRoot());
        } catch (e) {}
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
      this._autoFrame(asset);
    }

    _autoFrame(asset) {
      try {
        var bbox = asset.getBoundingBox();
        var cx = (bbox.min[0] + bbox.max[0]) / 2;
        var cy = (bbox.min[1] + bbox.max[1]) / 2;
        var cz = (bbox.min[2] + bbox.max[2]) / 2;
        var maxDim = Math.max(bbox.max[0] - bbox.min[0], bbox.max[1] - bbox.min[1], bbox.max[2] - bbox.min[2]);
        if (maxDim > 0) {
          this._orbitTarget = [cx, cy, cz];
          this._orbitRadius = maxDim * 1.8;
          this._orbitHeight = cy;
        }
      } catch (e) {}
    }

    addModel(url) {
      var self = this;
      return new Promise(function(resolve, reject) {
        fetch(url)
          .then(function(resp) { return resp.arrayBuffer(); })
          .then(function(buffer) {
            try {
              var asset = self._loader.createAsset(new Uint8Array(buffer));
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

    loadGLBBuffer(buffer) {
      var asset = this._loader.createAsset(buffer);
      if (!asset) return null;
      asset.loadResources();
      this._scene.addEntity(asset.getRoot());
      this._scene.addEntities(asset.getRenderableEntities());
      return asset;
    }

    removeAsset(asset) {
      if (!asset) return;
      try {
        asset.getRenderableEntities().forEach(function(e) { this._scene.remove(e); }.bind(this));
        this._scene.remove(asset.getRoot());
      } catch (e) {}
    }

    get engine() { return this._engine; }
    get scene() { return this._scene; }
    get view() { return this._view; }
    get camera() { return this._camera; }

    // ── Basic setters ──

    setAutoRotate(enabled) { this._autoRotate = enabled; this._wantsAutoRotate = enabled; return this; }
    setCameraDistance(d) { this._orbitRadius = d; return this; }

    setBackgroundColor(r, g, b, a) {
      this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true });
      return this;
    }

    // ── Post-processing ──

    setBloom(opts) {
      try { this._view.setBloomOptions(Object.assign({ enabled: true, strength: 0.1, resolution: 360, levels: 6, blendMode: 0 }, opts)); } catch (e) {}
      return this;
    }

    setFog(opts) {
      try { this._view.setFogOptions(Object.assign({ enabled: true, distance: 10, maximumOpacity: 0.8, color: [0.5, 0.55, 0.65] }, opts)); } catch (e) {}
      return this;
    }

    setVignette(opts) {
      try { this._view.setVignetteOptions(Object.assign({ enabled: true, midPoint: 0.5, roundness: 0.5, feather: 0.5 }, opts)); } catch (e) {}
      return this;
    }

    setDOF(opts) {
      try { this._view.setDepthOfFieldOptions(Object.assign({ enabled: true, cocScale: 1.0 }, opts)); } catch (e) {}
      return this;
    }

    setMSAA(opts) {
      try { this._view.setMultiSampleAntiAliasingOptions(Object.assign({ enabled: true, sampleCount: 4 }, opts)); } catch (e) {}
      return this;
    }

    setTAA(opts) {
      try { this._view.setTemporalAntiAliasingOptions(Object.assign({ enabled: true }, opts)); } catch (e) {}
      return this;
    }

    setSSR(opts) {
      try { this._view.setScreenSpaceReflectionsOptions(Object.assign({ enabled: true }, opts)); } catch (e) {}
      return this;
    }

    setQuality(level) {
      if (level === 'low') {
        this.setBloom({ enabled: false }); this.setMSAA({ enabled: false });
      } else if (level === 'medium') {
        this.setBloom({ enabled: true, strength: 0.05 }); this.setMSAA({ enabled: true, sampleCount: 2 });
      } else if (level === 'high') {
        this.setBloom({ enabled: true, strength: 0.1 }); this.setMSAA({ enabled: true, sampleCount: 4 }); this.setVignette({ enabled: true });
      } else if (level === 'ultra') {
        this.setBloom({ enabled: true, strength: 0.15 }); this.setMSAA({ enabled: true, sampleCount: 4 });
        this.setTAA({ enabled: true }); this.setSSR({ enabled: true }); this.setVignette({ enabled: true });
      }
      return this;
    }

    // ── Camera helpers ──

    setCameraPosition(x, y, z) {
      this._orbitRadius = Math.sqrt(x * x + z * z);
      this._orbitHeight = y;
      this._angle = Math.atan2(x, z);
      return this;
    }

    setCameraTarget(x, y, z) { this._orbitTarget = [x, y, z]; return this; }

    setCameraFOV(degrees) {
      this._fov = degrees;
      var c = this._canvas;
      this._camera.setProjectionFov(degrees, c.width / c.height, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
      return this;
    }

    setOrbitSpeed(speed) { this._orbitSpeed = speed; return this; }

    setOrbitLimits(opts) {
      if (opts.minDistance !== undefined) this._minRadius = opts.minDistance;
      if (opts.maxDistance !== undefined) this._maxRadius = opts.maxDistance;
      return this;
    }

    animateCamera(opts) {
      var self = this;
      var duration = opts.duration || 1000;
      var startTime = performance.now();
      var sA = this._angle, sR = this._orbitRadius, sH = this._orbitHeight;
      var sT = this._orbitTarget.slice();
      var eT = opts.target || sT;
      var eR = opts.distance !== undefined ? opts.distance : sR;
      var eH = opts.height !== undefined ? opts.height : sH;
      var eA = opts.angle !== undefined ? opts.angle : sA;
      var wasAuto = this._autoRotate;
      this._autoRotate = false;
      this._cameraAnimating = true;

      function ease(t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
      function lerp(a, b, t) { return a + (b - a) * t; }

      function step(now) {
        if (!self._running || !self._cameraAnimating) return;
        var t = ease(Math.min((now - startTime) / duration, 1));
        self._angle = lerp(sA, eA, t);
        self._orbitRadius = lerp(sR, eR, t);
        self._orbitHeight = lerp(sH, eH, t);
        self._orbitTarget = [lerp(sT[0], eT[0], t), lerp(sT[1], eT[1], t), lerp(sT[2], eT[2], t)];
        if (t < 1) { requestAnimationFrame(step); }
        else { self._cameraAnimating = false; if (wasAuto) self._autoRotate = true; if (opts.onComplete) opts.onComplete(); }
      }
      requestAnimationFrame(step);
      return this;
    }

    // ── Dynamic lights ──

    addLight(type, opts) {
      opts = opts || {};
      var entity = Filament.EntityManager.get().create();
      var typeMap = { sun: Filament.LightManager$Type.SUN, directional: Filament.LightManager$Type.DIRECTIONAL, point: Filament.LightManager$Type.POINT, spot: Filament.LightManager$Type.SPOT };
      var builder = Filament.LightManager.Builder(typeMap[type] || Filament.LightManager$Type.POINT)
        .color(opts.color || [1, 1, 1]).intensity(opts.intensity || 100000);
      if (opts.direction) builder.direction(opts.direction);
      if (opts.position) builder.position(opts.position);
      if (opts.falloff) builder.falloff(opts.falloff);
      if (opts.castShadows) builder.castShadows(true);
      if (type === 'spot') builder.spotLightCone(opts.innerCone || 0.5, opts.outerCone || 0.7);
      if (type === 'sun') { builder.sunAngularRadius(opts.angularRadius || 1.9); builder.sunHaloSize(opts.haloSize || 10.0); builder.sunHaloFalloff(opts.haloFalloff || 80.0); }
      builder.build(this._engine, entity);
      this._scene.addEntity(entity);
      this._userLights.push(entity);
      return entity;
    }

    removeLight(entity) {
      this._scene.remove(entity);
      var i = this._userLights.indexOf(entity);
      if (i >= 0) this._userLights.splice(i, 1);
      return this;
    }

    clearLights() {
      this._userLights.forEach(function(e) { this._scene.remove(e); }.bind(this));
      this._userLights = [];
      return this;
    }

    // ── Skybox control ──

    setSkyboxColor(r, g, b) {
      try { this._scene.setSkybox(Filament.Skybox.Builder().color([r, g, b, 1]).build(this._engine)); } catch (e) {}
      return this;
    }

    removeSkybox() { try { this._scene.setSkybox(null); } catch (e) {} return this; }

    // ── Asset querying ──

    getEntitiesByName(name) { if (!this._asset) return []; try { return this._asset.getEntitiesByName(name); } catch (e) { return []; } }
    getEntitiesByPrefix(prefix) { if (!this._asset) return []; try { return this._asset.getEntitiesByPrefix(prefix); } catch (e) { return []; } }

    // ── Lifecycle ──

    dispose() {
      this._running = false;
      if (this._resizeObserver) this._resizeObserver.disconnect();
      _activeCanvases.delete(this._canvas);
      try { Filament.Engine.destroy(this._engine); } catch (e) {}
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
        var minR = self._minRadius || 0.5;
        var maxR = self._maxRadius || 50;
        self._orbitRadius = Math.max(minR, Math.min(maxR, self._orbitRadius));
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
        var dpr = Math.min(devicePixelRatio, 2);
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
        if (self._autoRotate) self._angle += (self._orbitSpeed || 0.00873);
        if (!self._isDragging && !self._cameraAnimating) {
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
          t, [0, 1, 0]
        );
        self._engine.execute();
        try {
          if (self._renderer.beginFrame(self._swapChain)) {
            self._renderer.renderView(self._view);
            self._renderer.endFrame();
          }
        } catch (e) {
          console.error('SceneView render error:', e.message);
          self._running = false;
        }
        requestAnimationFrame(render);
      }
      render();
    }
  }

  var _activeCanvases = new Set();

  function _createEngine(canvasOrId, options) {
    options = options || {};
    var canvas = typeof canvasOrId === 'string' ? document.getElementById(canvasOrId) : canvasOrId;
    if (!canvas) throw new Error('Canvas not found: ' + canvasOrId);
    if (_activeCanvases.has(canvas)) { console.warn('SceneView: Canvas already initialized, skipping'); return null; }
    _activeCanvases.add(canvas);

    var dpr = Math.min(devicePixelRatio, 2);
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

    try { view.setAmbientOcclusionOptions({ enabled: true, radius: 0.3, bias: 0.0005, intensity: 1.0, quality: 1 }); } catch (e) {}

    var sun = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.SUN)
      .color([0.98, 0.92, 0.89]).intensity(options.lightIntensity || 110000)
      .direction([0.6, -1.0, -0.8]).sunAngularRadius(1.9).sunHaloSize(10.0).sunHaloFalloff(80.0)
      .build(engine, sun);
    scene.addEntity(sun);

    var fill = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
      .color([0.7, 0.75, 0.9]).intensity(60000).direction([-0.5, 0.5, 1.0])
      .build(engine, fill);
    scene.addEntity(fill);

    var back = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
      .color([0.5, 0.6, 0.9]).intensity(50000).direction([0, 0.3, 1.0])
      .build(engine, back);
    scene.addEntity(back);

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
          if (options.skybox !== false) {
            try {
              var reflections = ibl.getReflectionsTexture();
              if (reflections) {
                scene.setSkybox(Filament.Skybox.Builder().environment(reflections).build(engine));
                console.log('SceneView: Skybox created from IBL cubemap');
              }
            } catch (skyErr) { console.log('SceneView: Skybox not available (IBL-only mode)'); }
          }
          console.log('SceneView: KTX IBL loaded (' + Math.round(buffer.length / 1024) + 'KB)');
        } catch (e) {
          console.warn('SceneView: createIblFromKtx1 failed, using SH fallback', e);
          _applySyntheticIBL(engine, scene);
        }
      })
      .catch(function() { _applySyntheticIBL(engine, scene); });

    var loader = engine.createAssetLoader();
    var instance = new SceneViewInstance(canvas, engine, scene, renderer, view, swapChain, camera, cameraEntity, loader);
    instance._fov = fov;

    if (options.autoRotate === false) instance.setAutoRotate(false);
    if (options.quality) instance.setQuality(options.quality);
    if (options.bloom) instance.setBloom(typeof options.bloom === 'object' ? options.bloom : {});
    if (options.fog) instance.setFog(typeof options.fog === 'object' ? options.fog : {});
    if (options.vignette) instance.setVignette(typeof options.vignette === 'object' ? options.vignette : {});
    if (options.dof) instance.setDOF(typeof options.dof === 'object' ? options.dof : {});
    if (options.msaa) instance.setMSAA(typeof options.msaa === 'object' ? options.msaa : {});

    return instance;
  }

  function _applySyntheticIBL(engine, scene) {
    try {
      var ibl = Filament.IndirectLight.Builder()
        .irradiance(3, [
           0.65,  0.65,  0.70,  0.10,  0.10,  0.12,  0.15,  0.15,  0.18,
          -0.02, -0.02, -0.01,  0.04,  0.04,  0.05,  0.08,  0.08,  0.10,
           0.01,  0.01,  0.01, -0.02, -0.02, -0.02,  0.03,  0.03,  0.03
        ])
        .intensity(35000).build(engine);
      scene.setIndirectLight(ibl);
      console.log('SceneView: Using synthetic SH IBL');
    } catch (e) {}
  }

  function create(canvasOrId, options) {
    return _ensureFilament().then(function() {
      return new Promise(function(resolve, reject) {
        if (typeof Filament.Engine !== 'undefined') {
          try {
            var instance = _createEngine(canvasOrId, options);
            if (instance) resolve(instance); else reject(new Error('SceneView: Canvas already initialized'));
          } catch (e) { reject(e); }
          return;
        }
        Filament.init([], function() {
          try {
            var instance = _createEngine(canvasOrId, options);
            if (instance) resolve(instance); else reject(new Error('SceneView: Canvas already initialized'));
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
    version: '1.5.0',
    create: create,
    modelViewer: modelViewer
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
