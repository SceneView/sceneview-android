/**
 * SceneView Web — Simple 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * Advanced PBR materials:
 *   var mat = sv.createMaterial({ baseColor: '#gold', metallic: 1.0, roughness: 0.2 });
 *   sv.setMaterial(entity, mat);
 *
 * Environment presets:
 *   sv.setEnvironment('studio');
 *   sv.setDynamicSky({ hour: 14, turbidity: 4 });
 *
 * Powered by Filament.js v1.70.1 (Google's PBR renderer, WASM).
 * https://sceneview.github.io
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

  // ─── Color utilities ────────────────────────────────────────────────────────

  /**
   * Parse a color value to [r, g, b, a] in linear space (0-1).
   * Accepts: [r,g,b], [r,g,b,a], '#hex', '#rrggbb', '#rrggbbaa', CSS named colors.
   */
  function _parseColor(value) {
    if (Array.isArray(value)) {
      return [value[0] || 0, value[1] || 0, value[2] || 0, value[3] !== undefined ? value[3] : 1.0];
    }
    if (typeof value === 'string') {
      var hex = value.replace('#', '');
      // Named color shortcuts
      var named = {
        'white': 'ffffff', 'black': '000000', 'red': 'ff0000', 'green': '00ff00',
        'blue': '0000ff', 'gold': 'ffd700', 'silver': 'c0c0c0', 'gray': '808080',
        'grey': '808080', 'orange': 'ff8c00', 'yellow': 'ffff00', 'cyan': '00ffff',
        'magenta': 'ff00ff', 'pink': 'ff69b4', 'brown': '8b4513', 'ivory': 'fffff0',
        'coral': 'ff7f50', 'salmon': 'fa8072', 'crimson': 'dc143c', 'navy': '000080',
        'teal': '008080', 'olive': '808000', 'maroon': '800000', 'aqua': '00ffff',
        'lime': '00ff00', 'purple': '800080', 'indigo': '4b0082', 'violet': 'ee82ee'
      };
      if (named[value.toLowerCase()]) hex = named[value.toLowerCase()];
      // 3-char hex
      if (hex.length === 3) hex = hex[0]+hex[0]+hex[1]+hex[1]+hex[2]+hex[2];
      if (hex.length >= 6) {
        var r = parseInt(hex.substring(0,2), 16) / 255;
        var g = parseInt(hex.substring(2,4), 16) / 255;
        var b = parseInt(hex.substring(4,6), 16) / 255;
        var a = hex.length >= 8 ? parseInt(hex.substring(6,8), 16) / 255 : 1.0;
        // sRGB to linear approximation
        return [Math.pow(r, 2.2), Math.pow(g, 2.2), Math.pow(b, 2.2), a];
      }
    }
    return [0.8, 0.8, 0.8, 1.0]; // default light grey
  }

  /** Check if a value is a texture URL string (not a color) */
  function _isTextureUrl(value) {
    if (typeof value !== 'string') return false;
    var lower = value.toLowerCase();
    return lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') ||
           lower.endsWith('.webp') || lower.endsWith('.ktx') || lower.endsWith('.ktx2') ||
           lower.indexOf('/') !== -1;  // path-like
  }

  // ─── Texture cache ─────────────────────────────────────────────────────────

  /** Global texture cache: URL -> Filament Texture */
  var _textureCache = {};

  /**
   * Load a texture from URL, returning a Promise<Filament.Texture>.
   * Uses an in-memory cache to avoid reloading the same texture.
   */
  function _loadTexture(engine, url) {
    if (_textureCache[url]) return Promise.resolve(_textureCache[url]);

    return new Promise(function(resolve, reject) {
      var img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = function() {
        try {
          // Draw to canvas to get pixel data
          var cvs = document.createElement('canvas');
          cvs.width = img.naturalWidth;
          cvs.height = img.naturalHeight;
          var ctx = cvs.getContext('2d');
          ctx.drawImage(img, 0, 0);
          var imageData = ctx.getImageData(0, 0, cvs.width, cvs.height);
          var pixels = new Uint8Array(imageData.data.buffer);

          var texture = Filament.Texture.Builder()
            .width(cvs.width)
            .height(cvs.height)
            .levels(1)
            .sampler(Filament.Texture$Sampler.SAMPLER_2D)
            .format(Filament.Texture$InternalFormat.RGBA8)
            .build(engine);

          var pixelBufferDescriptor = Filament.PixelBufferDescriptor(
            pixels,
            Filament.PixelDataFormat.RGBA,
            Filament.PixelDataType.UBYTE
          );
          texture.setImage(engine, 0, pixelBufferDescriptor);
          texture.generateMipmaps(engine);

          _textureCache[url] = texture;
          resolve(texture);
        } catch (e) {
          console.warn('SceneView: Failed to create texture from ' + url, e);
          reject(e);
        }
      };
      img.onerror = function() {
        reject(new Error('SceneView: Failed to load texture image: ' + url));
      };
      img.src = url;
    });
  }

  // ─── Environment presets (synthetic SH bands) ───────────────────────────────

  /**
   * Environment presets using 3-band spherical harmonics.
   * Each preset defines 9 SH coefficients (3 bands * 3 RGB channels = 27 floats)
   * that produce characteristic indirect lighting.
   */
  var _environmentPresets = {
    /** Neutral studio: even, slightly warm top light */
    studio: {
      irradiance: [
         0.65,  0.65,  0.70,    // L00 — ambient base
         0.10,  0.10,  0.12,    // L1-1
         0.15,  0.15,  0.18,    // L10  — slight top bias
        -0.02, -0.02, -0.01,    // L11
         0.04,  0.04,  0.05,    // L2-2
         0.08,  0.08,  0.10,    // L2-1
         0.01,  0.01,  0.01,    // L20
        -0.02, -0.02, -0.02,    // L21
         0.03,  0.03,  0.03     // L22
      ],
      intensity: 35000,
      skyColor: [0.05, 0.06, 0.1, 1.0]
    },

    /** Bright outdoor: strong blue sky, warm ground bounce */
    outdoor: {
      irradiance: [
         0.90,  0.95,  1.10,    // L00 — bright sky blue tint
         0.05,  0.05,  0.08,    // L1-1
         0.35,  0.35,  0.40,    // L10  — strong top light (sky)
        -0.05, -0.03, -0.01,    // L11  — warm side fill
         0.08,  0.06,  0.04,    // L2-2
         0.15,  0.12,  0.08,    // L2-1 — ground bounce warm
         0.02,  0.02,  0.02,    // L20
        -0.04, -0.03, -0.02,    // L21
         0.06,  0.05,  0.04     // L22
      ],
      intensity: 55000,
      skyColor: [0.4, 0.6, 0.95, 1.0]
    },

    /** Night: cool, dark, moonlit */
    night: {
      irradiance: [
         0.10,  0.12,  0.20,    // L00 — dark blue ambient
         0.01,  0.01,  0.02,    // L1-1
         0.04,  0.05,  0.10,    // L10  — slight overhead moon
        -0.01, -0.01,  0.00,    // L11
         0.01,  0.01,  0.02,    // L2-2
         0.02,  0.02,  0.04,    // L2-1
         0.00,  0.00,  0.01,    // L20
        -0.01, -0.01, -0.01,    // L21
         0.01,  0.01,  0.01     // L22
      ],
      intensity: 8000,
      skyColor: [0.01, 0.01, 0.03, 1.0]
    },

    /** Neutral flat: perfectly uniform white light, no directional bias */
    neutral: {
      irradiance: [
         0.80,  0.80,  0.80,    // L00 — uniform white
         0.00,  0.00,  0.00,    // L1-1 — no directional bias
         0.00,  0.00,  0.00,    // L10
         0.00,  0.00,  0.00,    // L11
         0.00,  0.00,  0.00,    // L2-2
         0.00,  0.00,  0.00,    // L2-1
         0.00,  0.00,  0.00,    // L20
         0.00,  0.00,  0.00,    // L21
         0.00,  0.00,  0.00     // L22
      ],
      intensity: 30000,
      skyColor: [0.9, 0.9, 0.9, 1.0]
    },

    /** Sunset: warm orange sky, purple shadows */
    sunset: {
      irradiance: [
         0.85,  0.55,  0.35,    // L00 — warm orange ambient
         0.10,  0.04,  0.02,    // L1-1
         0.20,  0.12,  0.08,    // L10  — overhead warm
        -0.08, -0.02,  0.04,    // L11  — side purple shadow
         0.06,  0.02,  0.01,    // L2-2
         0.12,  0.06,  0.03,    // L2-1
         0.01,  0.01,  0.01,    // L20
        -0.03, -0.01,  0.02,    // L21
         0.05,  0.03,  0.02     // L22
      ],
      intensity: 40000,
      skyColor: [0.6, 0.25, 0.1, 1.0]
    },

    /** Dawn: cool pink-blue transitional light */
    dawn: {
      irradiance: [
         0.50,  0.45,  0.65,    // L00 — cool pink-blue
         0.05,  0.03,  0.06,    // L1-1
         0.15,  0.12,  0.20,    // L10  — overhead cool
        -0.03, -0.01,  0.02,    // L11
         0.04,  0.03,  0.05,    // L2-2
         0.08,  0.06,  0.10,    // L2-1
         0.01,  0.01,  0.01,    // L20
        -0.02, -0.01,  0.01,    // L21
         0.03,  0.02,  0.04     // L22
      ],
      intensity: 25000,
      skyColor: [0.35, 0.25, 0.55, 1.0]
    }
  };

  // ─── Dynamic sky computation ────────────────────────────────────────────────

  /**
   * Generate SH irradiance coefficients from sky parameters.
   * Approximates Preetham/Hosek atmospheric model using simplified physics.
   *
   * @param {number} hour - Solar time (0-24), 12 = noon
   * @param {number} turbidity - Atmospheric haze (1.0 = clear, 10.0 = hazy)
   * @param {number} groundAlbedo - Ground reflectance (0-1)
   * @returns {{ irradiance: number[], intensity: number, sunDirection: number[], sunColor: number[], skyColor: number[] }}
   */
  function _computeSkyFromSunPosition(hour, turbidity, groundAlbedo) {
    turbidity = turbidity || 4.0;
    groundAlbedo = groundAlbedo || 0.3;

    // Solar elevation angle from hour of day
    // 0h and 24h = nadir, 6h = sunrise, 12h = zenith, 18h = sunset
    var solarAngle = (hour - 6.0) / 12.0 * Math.PI; // 0 at 6:00, PI at 18:00
    var elevation = Math.sin(solarAngle);
    var clampedElevation = Math.max(elevation, -0.1);

    // Sun direction (Y-up coordinate system)
    var sunAzimuth = (hour < 12 ? -1 : 1) * Math.PI * 0.3;
    var sunDir = [
      Math.cos(solarAngle) * Math.sin(sunAzimuth),
      Math.max(elevation, 0.01),
      Math.cos(solarAngle) * Math.cos(sunAzimuth)
    ];
    var sunLen = Math.sqrt(sunDir[0]*sunDir[0] + sunDir[1]*sunDir[1] + sunDir[2]*sunDir[2]);
    sunDir = [sunDir[0]/sunLen, sunDir[1]/sunLen, sunDir[2]/sunLen];

    // Sun color: warm at horizon (Rayleigh scattering), white at zenith
    var horizonFactor = 1.0 - Math.min(Math.max(elevation, 0), 1.0);
    var turbidityFade = 1.0 - (turbidity - 1.0) / 18.0;
    var sunR = 1.0;
    var sunG = 0.95 - horizonFactor * 0.35 - (turbidity - 2) * 0.02;
    var sunB = 0.90 - horizonFactor * 0.60 - (turbidity - 2) * 0.04;
    sunG = Math.max(sunG, 0.2);
    sunB = Math.max(sunB, 0.1);

    // Sky color: blue at zenith, orange at horizon, dark at night
    var dayFactor = Math.max(Math.min(elevation * 2, 1), 0);
    var skyR = (0.15 + horizonFactor * 0.45) * dayFactor;
    var skyG = (0.25 + horizonFactor * 0.20) * dayFactor;
    var skyB = (0.60 - horizonFactor * 0.30) * dayFactor;
    // Night sky
    if (elevation < 0) {
      var nightFade = Math.max(1.0 + elevation * 5, 0);
      skyR = skyR * nightFade + 0.01 * (1 - nightFade);
      skyG = skyG * nightFade + 0.01 * (1 - nightFade);
      skyB = skyB * nightFade + 0.03 * (1 - nightFade);
    }

    // Intensity: strong at noon, zero at night
    var intensityBase = 60000;
    var intensityFactor = Math.max(clampedElevation, 0);
    intensityFactor = Math.pow(intensityFactor, 0.6); // softer falloff
    var intensity = intensityBase * intensityFactor * turbidityFade;
    intensity = Math.max(intensity, elevation < 0 ? 2000 : 5000); // min ambient

    // Ground bounce contribution
    var groundR = skyR * groundAlbedo * 0.3;
    var groundG = skyG * groundAlbedo * 0.3;
    var groundB = skyB * groundAlbedo * 0.3;

    // Compute SH bands
    var ambient = Math.max(dayFactor * 0.6, 0.08);
    var topBias = Math.max(clampedElevation * 0.4, 0);
    var sideBias = horizonFactor * 0.08 * dayFactor;

    var irradiance = [
      // L00 — ambient base (sky color + ground bounce)
      skyR * 0.8 + groundR + 0.05, skyG * 0.7 + groundG + 0.05, skyB * 0.9 + groundB + 0.08,
      // L1-1 — horizontal variation
      sideBias * sunR, sideBias * sunG * 0.8, sideBias * sunB * 0.6,
      // L10 — vertical (sky vs ground)
      topBias * 0.8 + ambient * 0.2, topBias * 0.7 + ambient * 0.2, topBias * 0.9 + ambient * 0.3,
      // L11 — directional
      -sunDir[0] * topBias * 0.1, -sunDir[0] * topBias * 0.08, -sunDir[0] * topBias * 0.06,
      // L2-2
      0.03 * dayFactor, 0.02 * dayFactor, 0.02 * dayFactor,
      // L2-1 — ground bounce
      groundR * 2, groundG * 2, groundB * 2,
      // L20
      0.01 * dayFactor, 0.01 * dayFactor, 0.01 * dayFactor,
      // L21
      -0.02 * dayFactor, -0.015 * dayFactor, -0.01 * dayFactor,
      // L22
      0.02 * dayFactor, 0.015 * dayFactor, 0.01 * dayFactor
    ];

    return {
      irradiance: irradiance,
      intensity: intensity,
      sunDirection: [-sunDir[0], -sunDir[1], -sunDir[2]], // Filament expects direction TO sun
      sunColor: [sunR, sunG, sunB],
      skyColor: [skyR, skyG, skyB, 1.0]
    };
  }

  // ─── SceneViewInstance ──────────────────────────────────────────────────────

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
      this._angle = 0.785; // Start at ~45 degrees like model-viewer
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
      // Material tracking
      this._materials = new Map();
      this._materialCounter = 0;
      // Environment state
      this._currentIbl = null;
      this._sunEntity = null;
      this._fillEntity = null;
      this._backEntity = null;
      this._environmentPreset = null;
      // Dynamic sky animation
      this._skyAnimation = null;
      // Reflection probes
      this._reflectionProbes = [];
      this._reflectionProbeCounter = 0;
      // Ground plane
      this._groundPlaneEntity = null;
      this._groundPlaneMaterial = null;
      // Exposure
      this._exposure = null;
      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

    // ── Model loading ─────────────────────────────────────────────────────────

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

    // ── Basic scene controls ──────────────────────────────────────────────────

    setAutoRotate(enabled) { this._autoRotate = enabled; this._wantsAutoRotate = enabled; return this; }
    setCameraDistance(d) { this._orbitRadius = d; return this; }

    setBackgroundColor(r, g, b, a) {
      this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true });
      return this;
    }

    dispose() {
      this._running = false;
      if (this._skyAnimation) { this._skyAnimation.cancelled = true; this._skyAnimation = null; }
      if (this._resizeObserver) this._resizeObserver.disconnect();
      try { Filament.Engine.destroy(this._engine); } catch (e) { /* already destroyed */ }
    }

    // ── PBR Material System ───────────────────────────────────────────────────

    /**
     * Create a PBR material from options.
     *
     * @param {Object} options — PBR material properties
     * @param {Array|string} [options.baseColor] — [r,g,b,a], '#hex', 'color name', or texture URL
     * @param {number|string} [options.metallic=0.0] — 0-1 or metallic map URL
     * @param {number|string} [options.roughness=0.5] — 0-1 or roughness map URL
     * @param {string} [options.normal] — Normal map texture URL
     * @param {string} [options.occlusion] — Ambient occlusion map URL
     * @param {Array|string} [options.emissive] — [r,g,b] or emissive map URL
     * @param {number} [options.emissiveStrength=1.0] — Emissive intensity multiplier
     * @param {number} [options.opacity=1.0] — Opacity (0 = transparent, 1 = opaque)
     * @param {boolean} [options.doubleSided=false] — Render both sides
     * @param {boolean} [options.unlit=false] — Ignore lighting (flat shading)
     * @param {number} [options.transmission=0] — Glass/transmission (0 = opaque, 1 = fully transparent)
     * @param {number} [options.ior=1.5] — Index of refraction (glass = 1.5, water = 1.33, diamond = 2.42)
     * @param {number} [options.clearCoat=0] — Clear coat layer intensity
     * @param {number} [options.clearCoatRoughness=0] — Clear coat roughness
     * @param {Array} [options.sheenColor] — Sheen/fabric color [r,g,b]
     * @param {number} [options.sheenRoughness=0] — Sheen roughness
     * @returns {{ id: number, options: Object, _textures: Object, ready: Promise }}
     */
    createMaterial(options) {
      options = options || {};
      var id = ++this._materialCounter;
      var self = this;

      // Resolve texture URLs to Filament textures
      var texturePromises = [];
      var textures = {};

      // Base color
      if (_isTextureUrl(options.baseColor)) {
        texturePromises.push(
          _loadTexture(self._engine, options.baseColor).then(function(tex) { textures.baseColorMap = tex; })
        );
      }
      // Metallic
      if (_isTextureUrl(options.metallic)) {
        texturePromises.push(
          _loadTexture(self._engine, options.metallic).then(function(tex) { textures.metallicMap = tex; })
        );
      }
      // Roughness
      if (_isTextureUrl(options.roughness)) {
        texturePromises.push(
          _loadTexture(self._engine, options.roughness).then(function(tex) { textures.roughnessMap = tex; })
        );
      }
      // Normal map
      if (options.normal && _isTextureUrl(options.normal)) {
        texturePromises.push(
          _loadTexture(self._engine, options.normal).then(function(tex) { textures.normalMap = tex; })
        );
      }
      // Occlusion map
      if (options.occlusion && _isTextureUrl(options.occlusion)) {
        texturePromises.push(
          _loadTexture(self._engine, options.occlusion).then(function(tex) { textures.occlusionMap = tex; })
        );
      }
      // Emissive
      if (_isTextureUrl(options.emissive)) {
        texturePromises.push(
          _loadTexture(self._engine, options.emissive).then(function(tex) { textures.emissiveMap = tex; })
        );
      }

      var material = {
        id: id,
        options: options,
        _textures: textures,
        ready: texturePromises.length > 0
          ? Promise.all(texturePromises).then(function() { return material; })
          : Promise.resolve(material)
      };

      this._materials.set(id, material);
      return material;
    }

    /**
     * Apply a material to a renderable entity.
     * This modifies the entity's MaterialInstance parameters.
     *
     * For glTF assets, use with entities from asset.getRenderableEntities().
     * Parameters are applied via Filament's MaterialInstance API.
     *
     * @param {number} entity — Filament entity
     * @param {Object} material — Material created by createMaterial()
     */
    setMaterial(entity, material) {
      var self = this;
      var opts = material.options;

      // Wait for textures to load, then apply
      material.ready.then(function() {
        try {
          var rm = self._engine.getRenderableManager();
          var ri = rm.getInstance(entity);
          if (!ri) {
            console.warn('SceneView: Entity is not renderable, cannot set material');
            return;
          }

          var mi = rm.getMaterialInstanceAt(ri, 0);
          if (!mi) {
            console.warn('SceneView: No MaterialInstance found on entity');
            return;
          }

          // Base color
          if (opts.baseColor && !_isTextureUrl(opts.baseColor)) {
            var bc = _parseColor(opts.baseColor);
            if (opts.opacity !== undefined && opts.opacity < 1.0) bc[3] = opts.opacity;
            try { mi.setColor4Parameter('baseColorFactor', Filament.RgbaType.sRGB, bc); } catch(e) {
              try { mi.setFloat4Parameter('baseColorFactor', bc); } catch(e2) { /* skip */ }
            }
          }
          if (material._textures.baseColorMap) {
            try {
              var sampler = new Filament.TextureSampler(
                Filament.MinFilter.LINEAR_MIPMAP_LINEAR,
                Filament.MagFilter.LINEAR,
                Filament.WrapMode.REPEAT
              );
              mi.setTextureParameter('baseColorMap', material._textures.baseColorMap, sampler);
            } catch(e) { /* glTF mat may not have this param */ }
          }

          // Metallic
          if (typeof opts.metallic === 'number') {
            try { mi.setFloatParameter('metallicFactor', opts.metallic); } catch(e) { /* skip */ }
          }
          if (material._textures.metallicMap) {
            try {
              var sampler2 = new Filament.TextureSampler(
                Filament.MinFilter.LINEAR_MIPMAP_LINEAR,
                Filament.MagFilter.LINEAR,
                Filament.WrapMode.REPEAT
              );
              mi.setTextureParameter('metallicRoughnessMap', material._textures.metallicMap, sampler2);
            } catch(e) { /* skip */ }
          }

          // Roughness
          if (typeof opts.roughness === 'number') {
            try { mi.setFloatParameter('roughnessFactor', opts.roughness); } catch(e) { /* skip */ }
          }

          // Normal map
          if (material._textures.normalMap) {
            try {
              var sampler3 = new Filament.TextureSampler(
                Filament.MinFilter.LINEAR_MIPMAP_LINEAR,
                Filament.MagFilter.LINEAR,
                Filament.WrapMode.REPEAT
              );
              mi.setTextureParameter('normalMap', material._textures.normalMap, sampler3);
            } catch(e) { /* skip */ }
          }

          // Occlusion map
          if (material._textures.occlusionMap) {
            try {
              var sampler4 = new Filament.TextureSampler(
                Filament.MinFilter.LINEAR_MIPMAP_LINEAR,
                Filament.MagFilter.LINEAR,
                Filament.WrapMode.REPEAT
              );
              mi.setTextureParameter('occlusionMap', material._textures.occlusionMap, sampler4);
            } catch(e) { /* skip */ }
          }

          // Emissive
          if (opts.emissive && !_isTextureUrl(opts.emissive)) {
            var ec = _parseColor(opts.emissive);
            var strength = opts.emissiveStrength || 1.0;
            try { mi.setFloat3Parameter('emissiveFactor', [ec[0]*strength, ec[1]*strength, ec[2]*strength]); } catch(e) { /* skip */ }
          }
          if (material._textures.emissiveMap) {
            try {
              var sampler5 = new Filament.TextureSampler(
                Filament.MinFilter.LINEAR_MIPMAP_LINEAR,
                Filament.MagFilter.LINEAR,
                Filament.WrapMode.REPEAT
              );
              mi.setTextureParameter('emissiveMap', material._textures.emissiveMap, sampler5);
            } catch(e) { /* skip */ }
          }

          // Transmission (glass)
          if (typeof opts.transmission === 'number' && opts.transmission > 0) {
            try { mi.setFloatParameter('transmissionFactor', opts.transmission); } catch(e) { /* skip */ }
          }
          if (typeof opts.ior === 'number') {
            try { mi.setFloatParameter('ior', opts.ior); } catch(e) { /* skip */ }
          }

          // Clear coat
          if (typeof opts.clearCoat === 'number' && opts.clearCoat > 0) {
            try { mi.setFloatParameter('clearCoatFactor', opts.clearCoat); } catch(e) { /* skip */ }
            if (typeof opts.clearCoatRoughness === 'number') {
              try { mi.setFloatParameter('clearCoatRoughnessFactor', opts.clearCoatRoughness); } catch(e) { /* skip */ }
            }
          }

          // Sheen (fabric)
          if (opts.sheenColor) {
            var sc = _parseColor(opts.sheenColor);
            try { mi.setFloat3Parameter('sheenColorFactor', [sc[0], sc[1], sc[2]]); } catch(e) { /* skip */ }
            if (typeof opts.sheenRoughness === 'number') {
              try { mi.setFloatParameter('sheenRoughnessFactor', opts.sheenRoughness); } catch(e) { /* skip */ }
            }
          }

          // Double sided
          if (opts.doubleSided) {
            try { mi.setDoubleSided(true); } catch(e) { /* skip */ }
          }

          console.log('SceneView: Material applied to entity');
        } catch(e) {
          console.warn('SceneView: Failed to apply material', e);
        }
      });

      return this;
    }

    /**
     * Get the current material info for an entity.
     * Returns the MaterialInstance parameters as readable object.
     *
     * @param {number} entity — Filament entity
     * @returns {Object|null} Material info or null
     */
    getMaterial(entity) {
      try {
        var rm = this._engine.getRenderableManager();
        var ri = rm.getInstance(entity);
        if (!ri) return null;
        var mi = rm.getMaterialInstanceAt(ri, 0);
        if (!mi) return null;
        return { _instance: mi, entity: entity };
      } catch(e) {
        return null;
      }
    }

    // ── Material Variants ─────────────────────────────────────────────────────

    /**
     * Get available material variant names from a glTF asset.
     * Many glTF models include variants (e.g., color options, texture swaps).
     *
     * @param {Object} [asset] — Filament asset (defaults to current loaded asset)
     * @returns {string[]} Array of variant names
     */
    getMaterialVariants(asset) {
      asset = asset || this._asset;
      if (!asset) return [];
      try {
        var count = asset.getMaterialVariantCount();
        var variants = [];
        for (var i = 0; i < count; i++) {
          variants.push(asset.getMaterialVariantName(i));
        }
        return variants;
      } catch(e) {
        console.warn('SceneView: Cannot read material variants', e);
        return [];
      }
    }

    /**
     * Switch to a named material variant on a glTF asset.
     *
     * @param {string} variantName — Name of the variant to activate
     * @param {Object} [asset] — Filament asset (defaults to current loaded asset)
     * @returns {this}
     */
    setMaterialVariant(variantName, asset) {
      asset = asset || this._asset;
      if (!asset) {
        console.warn('SceneView: No asset loaded, cannot set variant');
        return this;
      }
      try {
        var count = asset.getMaterialVariantCount();
        for (var i = 0; i < count; i++) {
          if (asset.getMaterialVariantName(i) === variantName) {
            asset.applyMaterialVariant(i);
            console.log('SceneView: Applied material variant "' + variantName + '"');
            return this;
          }
        }
        console.warn('SceneView: Variant "' + variantName + '" not found. Available: ' + this.getMaterialVariants(asset).join(', '));
      } catch(e) {
        console.warn('SceneView: Failed to set material variant', e);
      }
      return this;
    }

    // ── Environment System ────────────────────────────────────────────────────

    /**
     * Set the scene environment (skybox + IBL lighting).
     *
     * Accepts a preset name string or an options object:
     *   sv.setEnvironment('studio')
     *   sv.setEnvironment({ skybox: 'path/to/skybox.ktx', ibl: 'path/to/ibl.ktx', intensity: 40000 })
     *
     * Preset names: 'studio', 'outdoor', 'night', 'neutral', 'sunset', 'dawn'
     *
     * Options object:
     * @param {string|Array|Object} envOrOptions — Preset name or options object
     * @param {string|Array} [options.skybox] — KTX path, [r,g,b] solid color, or 'procedural'
     * @param {string} [options.ibl] — KTX path for IBL, or 'auto' to match skybox
     * @param {number} [options.intensity=30000] — IBL intensity (lux)
     * @param {number} [options.rotation=0] — IBL rotation in degrees
     * @returns {this}
     */
    setEnvironment(envOrOptions) {
      var self = this;

      // String preset
      if (typeof envOrOptions === 'string') {
        var preset = _environmentPresets[envOrOptions.toLowerCase()];
        if (!preset) {
          console.warn('SceneView: Unknown environment preset "' + envOrOptions + '". Available: ' +
            Object.keys(_environmentPresets).join(', '));
          return this;
        }
        this._environmentPreset = envOrOptions.toLowerCase();
        this._applyIblFromSH(preset.irradiance, preset.intensity);
        this.setBackgroundColor(preset.skyColor[0], preset.skyColor[1], preset.skyColor[2], preset.skyColor[3]);
        console.log('SceneView: Environment set to "' + envOrOptions + '"');
        return this;
      }

      // Options object
      var options = envOrOptions || {};
      var intensity = options.intensity || 30000;
      var rotation = (options.rotation || 0) * Math.PI / 180;

      // IBL from KTX file
      if (options.ibl && options.ibl !== 'auto') {
        fetch(options.ibl)
          .then(function(r) { return r.arrayBuffer(); })
          .then(function(ab) {
            try {
              var ibl = self._engine.createIblFromKtx1(new Uint8Array(ab));
              ibl.setIntensity(intensity);
              if (rotation !== 0) {
                try { ibl.setRotation([Math.cos(rotation), 0, Math.sin(rotation), 0, 1, 0, -Math.sin(rotation), 0, Math.cos(rotation)]); } catch(e) { /* skip */ }
              }
              self._scene.setIndirectLight(ibl);
              self._currentIbl = ibl;
              console.log('SceneView: IBL loaded from ' + options.ibl);
            } catch(e) {
              console.warn('SceneView: IBL load failed, using fallback', e);
              _applySyntheticIBL(self._engine, self._scene);
            }
          })
          .catch(function() { _applySyntheticIBL(self._engine, self._scene); });
      }

      // Skybox
      if (options.skybox) {
        if (Array.isArray(options.skybox)) {
          // Solid color background
          var c = options.skybox;
          self.setBackgroundColor(c[0], c[1], c[2], c[3] !== undefined ? c[3] : 1.0);
        } else if (typeof options.skybox === 'string' && options.skybox !== 'procedural') {
          // KTX skybox
          fetch(options.skybox)
            .then(function(r) { return r.arrayBuffer(); })
            .then(function(ab) {
              try {
                var skybox = self._engine.createSkyFromKtx1(new Uint8Array(ab));
                self._scene.setSkybox(skybox);
                console.log('SceneView: Skybox loaded from ' + options.skybox);
                // If IBL is 'auto', generate from same KTX
                if (options.ibl === 'auto') {
                  try {
                    var ibl = self._engine.createIblFromKtx1(new Uint8Array(ab));
                    ibl.setIntensity(intensity);
                    self._scene.setIndirectLight(ibl);
                    self._currentIbl = ibl;
                  } catch(e) { /* skip auto IBL */ }
                }
              } catch(e) {
                console.warn('SceneView: Skybox load failed', e);
              }
            })
            .catch(function(e) { console.warn('SceneView: Skybox fetch failed', e); });
        }
      }

      return this;
    }

    /**
     * Apply IBL from spherical harmonics coefficients.
     * @private
     */
    _applyIblFromSH(shCoeffs, intensity) {
      try {
        var ibl = Filament.IndirectLight.Builder()
          .irradiance(3, shCoeffs)
          .intensity(intensity)
          .build(this._engine);
        this._scene.setIndirectLight(ibl);
        this._currentIbl = ibl;
      } catch(e) {
        console.warn('SceneView: Failed to apply SH IBL', e);
      }
    }

    // ── Dynamic Sky ───────────────────────────────────────────────────────────

    /**
     * Set a procedural dynamic sky based on time of day.
     *
     * Creates atmospheric lighting by computing SH irradiance from solar position,
     * turbidity, and ground albedo. Updates both IBL and sun light.
     *
     * @param {Object} options
     * @param {number} options.hour — Solar time (0-24). 6=sunrise, 12=noon, 18=sunset
     * @param {number} [options.turbidity=4] — Atmospheric haze (1=clear, 10=hazy)
     * @param {number} [options.groundAlbedo=0.3] — Ground reflectance (0-1)
     * @returns {this}
     */
    setDynamicSky(options) {
      options = options || {};
      var hour = options.hour !== undefined ? options.hour : 12;
      var turbidity = options.turbidity || 4;
      var groundAlbedo = options.groundAlbedo || 0.3;

      var sky = _computeSkyFromSunPosition(hour, turbidity, groundAlbedo);

      // Apply IBL
      this._applyIblFromSH(sky.irradiance, sky.intensity);

      // Update sun light direction and color
      if (this._sunEntity) {
        try {
          var lm = this._engine.getLightManager();
          var li = lm.getInstance(this._sunEntity);
          if (li) {
            lm.setDirection(li, sky.sunDirection);
            lm.setColor(li, sky.sunColor);
            // Reduce sun intensity at horizon
            var elevation = Math.sin((hour - 6.0) / 12.0 * Math.PI);
            var sunIntensity = Math.max(elevation, 0) * 110000;
            sunIntensity = Math.max(sunIntensity, elevation < 0 ? 0 : 5000);
            lm.setIntensity(li, sunIntensity);
          }
        } catch(e) { /* skip */ }
      }

      // Update background color
      this.setBackgroundColor(sky.skyColor[0], sky.skyColor[1], sky.skyColor[2], sky.skyColor[3]);

      return this;
    }

    /**
     * Animate sky from one hour to another over a duration.
     * Creates a smooth day/night transition.
     *
     * @param {number} fromHour — Starting hour (0-24)
     * @param {number} toHour — Ending hour (0-24)
     * @param {number} duration — Duration in seconds
     * @param {Object} [options] — Optional turbidity, groundAlbedo
     * @returns {Promise} Resolves when animation completes
     */
    animateSky(fromHour, toHour, duration, options) {
      var self = this;
      options = options || {};
      var turbidity = options.turbidity || 4;
      var groundAlbedo = options.groundAlbedo || 0.3;

      // Cancel any existing sky animation
      if (this._skyAnimation) this._skyAnimation.cancelled = true;

      var animation = { cancelled: false };
      this._skyAnimation = animation;

      return new Promise(function(resolve) {
        var startTime = performance.now();
        var durationMs = duration * 1000;

        function tick() {
          if (animation.cancelled) { resolve(); return; }

          var elapsed = performance.now() - startTime;
          var t = Math.min(elapsed / durationMs, 1.0);
          // Smooth ease-in-out
          t = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;

          var currentHour = fromHour + (toHour - fromHour) * t;
          // Handle wrapping (e.g., 22 -> 4 should go 22->24->4, not 22->4)
          if (toHour < fromHour) {
            currentHour = fromHour + ((toHour + 24) - fromHour) * t;
            if (currentHour >= 24) currentHour -= 24;
          }

          self.setDynamicSky({ hour: currentHour, turbidity: turbidity, groundAlbedo: groundAlbedo });

          if (t < 1.0) {
            requestAnimationFrame(tick);
          } else {
            self._skyAnimation = null;
            resolve();
          }
        }
        tick();
      });
    }

    // ── Reflection Probes ─────────────────────────────────────────────────────

    /**
     * Add a zone-based reflection probe.
     * When the camera enters the zone, the probe's IBL is blended in.
     *
     * @param {Object} options
     * @param {number[]} options.position — [x, y, z] center of probe
     * @param {number[]|number} options.size — [w, h, d] box size or radius (sphere)
     * @param {string} [options.ibl] — KTX path for the probe's IBL
     * @param {number} [options.intensity=30000] — IBL intensity
     * @param {number} [options.blendDistance=1.0] — Distance over which to blend (meters)
     * @returns {{ id: number, position: number[], remove: Function }}
     */
    addReflectionProbe(options) {
      var self = this;
      options = options || {};
      var id = ++this._reflectionProbeCounter;

      var probe = {
        id: id,
        position: options.position || [0, 0, 0],
        size: Array.isArray(options.size) ? options.size : [options.size || 5, options.size || 5, options.size || 5],
        intensity: options.intensity || 30000,
        blendDistance: options.blendDistance || 1.0,
        ibl: null,
        _active: false,
        remove: function() { self.removeReflectionProbe(probe); }
      };

      // Load IBL if provided
      if (options.ibl) {
        fetch(options.ibl)
          .then(function(r) { return r.arrayBuffer(); })
          .then(function(ab) {
            try {
              probe.ibl = self._engine.createIblFromKtx1(new Uint8Array(ab));
              probe.ibl.setIntensity(probe.intensity);
              console.log('SceneView: Reflection probe ' + id + ' IBL loaded');
            } catch(e) {
              console.warn('SceneView: Reflection probe IBL failed', e);
            }
          })
          .catch(function(e) { console.warn('SceneView: Reflection probe IBL fetch failed', e); });
      } else {
        // Use synthetic IBL for this probe zone
        try {
          probe.ibl = Filament.IndirectLight.Builder()
            .irradiance(3, _environmentPresets.studio.irradiance)
            .intensity(probe.intensity)
            .build(self._engine);
        } catch(e) { /* skip */ }
      }

      this._reflectionProbes.push(probe);
      console.log('SceneView: Reflection probe ' + id + ' added at [' + probe.position.join(', ') + ']');

      return probe;
    }

    /**
     * Remove a reflection probe.
     * @param {Object} probe — Probe object returned by addReflectionProbe()
     */
    removeReflectionProbe(probe) {
      var idx = this._reflectionProbes.indexOf(probe);
      if (idx !== -1) {
        this._reflectionProbes.splice(idx, 1);
        console.log('SceneView: Reflection probe ' + probe.id + ' removed');
      }
      return this;
    }

    /**
     * Check if camera is inside any reflection probe zone and blend IBL.
     * Called each frame from the render loop.
     * @private
     */
    _updateReflectionProbes() {
      if (this._reflectionProbes.length === 0) return;

      // Current camera position
      var t = this._orbitTarget;
      var r = this._orbitRadius;
      var h = this._orbitHeight;
      var camX = t[0] + Math.sin(this._angle) * r;
      var camY = h;
      var camZ = t[2] + Math.cos(this._angle) * r;

      var closestProbe = null;
      var closestDist = Infinity;

      for (var i = 0; i < this._reflectionProbes.length; i++) {
        var probe = this._reflectionProbes[i];
        var p = probe.position;
        var s = probe.size;

        // Check if camera is inside box
        var dx = Math.abs(camX - p[0]);
        var dy = Math.abs(camY - p[1]);
        var dz = Math.abs(camZ - p[2]);

        if (dx < s[0] / 2 + probe.blendDistance &&
            dy < s[1] / 2 + probe.blendDistance &&
            dz < s[2] / 2 + probe.blendDistance) {
          var dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
          if (dist < closestDist) {
            closestDist = dist;
            closestProbe = probe;
          }
        }
      }

      // Apply closest probe IBL
      if (closestProbe && closestProbe.ibl && !closestProbe._active) {
        this._scene.setIndirectLight(closestProbe.ibl);
        closestProbe._active = true;
        // Deactivate others
        for (var j = 0; j < this._reflectionProbes.length; j++) {
          if (this._reflectionProbes[j] !== closestProbe) {
            this._reflectionProbes[j]._active = false;
          }
        }
      } else if (!closestProbe) {
        // Restore default IBL if we left all probes
        var anyActive = false;
        for (var k = 0; k < this._reflectionProbes.length; k++) {
          if (this._reflectionProbes[k]._active) {
            this._reflectionProbes[k]._active = false;
            anyActive = true;
          }
        }
        if (anyActive && this._currentIbl) {
          this._scene.setIndirectLight(this._currentIbl);
        }
      }
    }

    // ── Exposure / Tone Mapping ───────────────────────────────────────────────

    /**
     * Set camera exposure.
     *
     * @param {Object} options
     * @param {number} [options.aperture=16] — f-stop (1.4 to 22)
     * @param {number} [options.shutterSpeed=0.008] — Shutter speed in seconds (1/125 = 0.008)
     * @param {number} [options.sensitivity=100] — ISO sensitivity (100 to 6400)
     * @param {number} [options.ev] — EV override. If set, ignores aperture/shutter/sensitivity.
     * @returns {this}
     */
    setExposure(options) {
      options = options || {};
      try {
        if (options.ev !== undefined) {
          // EV mode: compute equivalent exposure
          // EV = log2(aperture^2 / shutterSpeed) - log2(sensitivity/100)
          // We reverse to set: aperture=1, shutter=2^(-EV), ISO=100
          var ev = options.ev;
          this._camera.setExposure(
            Math.pow(2, ev / 2),  // aperture
            1.0 / Math.pow(2, ev), // shutterSpeed
            100                     // sensitivity
          );
        } else {
          var aperture = options.aperture || 16.0;
          var shutterSpeed = options.shutterSpeed || (1.0 / 125.0);
          var sensitivity = options.sensitivity || 100.0;
          this._camera.setExposure(aperture, shutterSpeed, sensitivity);
        }
        this._exposure = options;
        console.log('SceneView: Exposure updated');
      } catch(e) {
        console.warn('SceneView: Failed to set exposure', e);
      }
      return this;
    }

    /**
     * Set the tone mapping operator.
     *
     * @param {string} type — 'aces' (default), 'filmic', 'linear', 'agx'
     * @returns {this}
     */
    setToneMapping(type) {
      try {
        var toneMapper;
        switch ((type || 'aces').toLowerCase()) {
          case 'linear':
            toneMapper = Filament.View$ToneMapping.LINEAR;
            break;
          case 'filmic':
            toneMapper = Filament.View$ToneMapping.FILMIC;
            break;
          case 'agx':
            // AGX may not be available in Filament.js 1.70; fall back to ACES
            toneMapper = Filament.View$ToneMapping.AGX || Filament.View$ToneMapping.ACES;
            break;
          case 'aces':
          default:
            toneMapper = Filament.View$ToneMapping.ACES;
            break;
        }
        this._view.setToneMapping(toneMapper);
        console.log('SceneView: Tone mapping set to "' + type + '"');
      } catch(e) {
        // Try via ColorGrading for newer Filament versions
        try {
          var cgType;
          switch ((type || 'aces').toLowerCase()) {
            case 'linear': cgType = Filament.ColorGrading$ToneMapping.LINEAR; break;
            case 'filmic': cgType = Filament.ColorGrading$ToneMapping.FILMIC; break;
            case 'agx': cgType = Filament.ColorGrading$ToneMapping.AGX || Filament.ColorGrading$ToneMapping.ACES; break;
            default: cgType = Filament.ColorGrading$ToneMapping.ACES; break;
          }
          var cg = Filament.ColorGrading.Builder()
            .toneMapping(cgType)
            .build(this._engine);
          this._view.setColorGrading(cg);
          console.log('SceneView: Tone mapping (ColorGrading) set to "' + type + '"');
        } catch(e2) {
          console.warn('SceneView: Failed to set tone mapping', e2);
        }
      }
      return this;
    }

    // ── Ground Plane / Shadow Receiver ────────────────────────────────────────

    /**
     * Set a ground plane that receives shadows.
     * Creates an invisible (or tinted) horizontal plane at the specified height.
     *
     * Uses Filament's built-in plane primitive with a translucent material
     * that only shows shadow darkening.
     *
     * @param {Object} options
     * @param {boolean} [options.enabled=true] — Show/hide the ground plane
     * @param {number} [options.height=0] — Y position of the ground
     * @param {boolean} [options.shadowOnly=true] — If true, only shadows visible (transparent otherwise)
     * @param {Array} [options.color=[0.5,0.5,0.5]] — Ground color (only visible if shadowOnly=false)
     * @param {number} [options.size=20] — Size of the ground plane in world units
     * @param {number} [options.opacity=0.3] — Shadow/ground opacity
     * @returns {this}
     */
    setGroundPlane(options) {
      options = options || {};
      var enabled = options.enabled !== false;
      var height = options.height || 0;
      var shadowOnly = options.shadowOnly !== false;
      var color = options.color || [0.5, 0.5, 0.5];
      var size = options.size || 20;
      var opacity = options.opacity !== undefined ? options.opacity : 0.3;

      // Remove existing ground plane
      if (this._groundPlaneEntity) {
        try {
          this._scene.remove(this._groundPlaneEntity);
          this._engine.destroyEntity(this._groundPlaneEntity);
        } catch(e) { /* skip */ }
        this._groundPlaneEntity = null;
      }

      if (!enabled) return this;

      try {
        // Create a simple quad using Filament's built-in plane geometry
        var entityManager = Filament.EntityManager.get();
        var entity = entityManager.create();

        // Vertex data for a horizontal plane (2 triangles)
        var halfSize = size / 2;
        var vertices = new Float32Array([
          -halfSize, height, -halfSize,  0, 1, 0,  0, 0,
           halfSize, height, -halfSize,  0, 1, 0,  1, 0,
           halfSize, height,  halfSize,  0, 1, 0,  1, 1,
          -halfSize, height,  halfSize,  0, 1, 0,  0, 1
        ]);
        var indices = new Uint16Array([0, 2, 1, 0, 3, 2]);

        // Create vertex buffer
        var vb = Filament.VertexBuffer.Builder()
          .vertexCount(4)
          .bufferCount(1)
          .attribute(Filament.VertexAttribute.POSITION, 0, Filament.VertexBuffer$AttributeType.FLOAT3, 0, 32)
          .attribute(Filament.VertexAttribute.TANGENTS, 0, Filament.VertexBuffer$AttributeType.FLOAT3, 12, 32)
          .attribute(Filament.VertexAttribute.UV0, 0, Filament.VertexBuffer$AttributeType.FLOAT2, 24, 32)
          .build(this._engine);
        vb.setBufferAt(this._engine, 0, vertices);

        // Create index buffer
        var ib = Filament.IndexBuffer.Builder()
          .indexCount(6)
          .bufferType(Filament.IndexBuffer$IndexType.USHORT)
          .build(this._engine);
        ib.setBuffer(this._engine, indices);

        // Build renderable
        Filament.RenderableManager.Builder(1)
          .boundingBox({ center: [0, height, 0], halfExtent: [halfSize, 0.01, halfSize] })
          .geometry(0, Filament.RenderableManager$PrimitiveType.TRIANGLES, vb, ib)
          .culling(false)
          .receiveShadows(true)
          .castShadows(false)
          .build(this._engine, entity);

        // Apply ground material properties
        try {
          var rm = this._engine.getRenderableManager();
          var ri = rm.getInstance(entity);
          if (ri) {
            var mi = rm.getMaterialInstanceAt(ri, 0);
            if (mi) {
              var a = shadowOnly ? opacity * 0.5 : opacity;
              try { mi.setColor4Parameter('baseColorFactor', Filament.RgbaType.sRGB, [color[0], color[1], color[2], a]); } catch(e) {
                try { mi.setFloat4Parameter('baseColorFactor', [color[0], color[1], color[2], a]); } catch(e2) { /* skip */ }
              }
              try { mi.setFloatParameter('roughnessFactor', 0.9); } catch(e) { /* skip */ }
              try { mi.setFloatParameter('metallicFactor', 0.0); } catch(e) { /* skip */ }
            }
          }
        } catch(e) { /* skip material setup */ }

        this._scene.addEntity(entity);
        this._groundPlaneEntity = entity;
        console.log('SceneView: Ground plane ' + (shadowOnly ? '(shadow-only)' : '') + ' at height ' + height);
      } catch(e) {
        console.warn('SceneView: Failed to create ground plane', e);
      }

      return this;
    }

    // ── Controls & rendering ──────────────────────────────────────────────────

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

        // Auto-rotate: 30 deg/sec / 60fps (matches model-viewer)
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

        // Update reflection probes based on camera position
        self._updateReflectionProbes();

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
      .castShadows(true)
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
    instance._sunEntity = sun;
    instance._fillEntity = fill;
    instance._backEntity = back;

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

  // ── Public API ──────────────────────────────────────────────────────────────

  global.SceneView = {
    version: '1.4.0',
    create: create,
    modelViewer: modelViewer,

    /** Available environment presets */
    environmentPresets: Object.keys(_environmentPresets),

    /**
     * Compute sky parameters for a given hour (utility function).
     * Useful for previewing dynamic sky without a SceneView instance.
     *
     * @param {number} hour — Solar time (0-24)
     * @param {number} [turbidity=4] — Atmospheric haze
     * @param {number} [groundAlbedo=0.3] — Ground reflectance
     * @returns {Object} Sky parameters including irradiance, intensity, sunDirection, sunColor, skyColor
     */
    computeSky: _computeSkyFromSunPosition,

    /** Color parsing utility */
    parseColor: _parseColor
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
