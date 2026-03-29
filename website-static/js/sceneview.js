/**
 * SceneView Web v2.0.0 — Full-featured 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * Scene graph with declarative builder:
 *   scene.build(root => {
 *     root.model('robot.glb', { position: [0, 0, 0] })
 *     root.group('furniture', group => {
 *       group.model('chair.glb', { position: [1, 0, 0] })
 *       group.model('table.glb', { position: [0, 0, 1] })
 *     })
 *     root.light('point', { position: [2, 3, 0], intensity: 10000 })
 *     root.cube({ size: [1,1,1], position: [3, 0, 0], color: [1, 0, 0] })
 *   })
 *
 * Features:
 *   - Scene graph (nodes, hierarchy, transforms)
 *   - Model loading (glTF/GLB)
 *   - Animation (playback, cross-fade, morph targets)
 *   - Procedural geometry (cube, sphere, cylinder, cone, plane, line, path)
 *   - PBR materials (baseColor, metallic, roughness, normal, transmission, clearCoat, sheen)
 *   - Environment presets (studio, outdoor, night, neutral, sunset, dawn)
 *   - Dynamic sky (Preetham atmospheric model)
 *   - Text, image, and video nodes (Canvas2D rendering)
 *   - Ray casting & collision detection (AABB, sphere, plane)
 *   - Gesture system (tap, double-tap, long-press, drag, pinch, rotate)
 *   - Entity selection with visual feedback
 *   - Billboard system
 *   - Reflection probes
 *   - Camera exposure & tone mapping
 *
 * Powered by Filament.js v1.70.1 (Google's PBR renderer, WASM).
 * https://sceneview.github.io
 *
 * @version 2.0.0
 * @license MIT
 */
(function(global) {
  'use strict';

  // Filament.js is loaded via <script> tag in HTML (js/filament/filament.js)

  /** Wait for Filament to be available (loaded by the script tag). */
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
    /** Return a new identity matrix */
    identity: function() {
      return new Float32Array([1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1]);
    },

    /** Multiply two 4x4 column-major matrices: out = a * b */
    multiply: function(a, b) {
      var out = new Float32Array(16);
      for (var col = 0; col < 4; col++) {
        for (var row = 0; row < 4; row++) {
          out[col * 4 + row] =
            a[0 * 4 + row] * b[col * 4 + 0] +
            a[1 * 4 + row] * b[col * 4 + 1] +
            a[2 * 4 + row] * b[col * 4 + 2] +
            a[3 * 4 + row] * b[col * 4 + 3];
        }
      }
      return out;
    },

    /** Transform a vec4 by a 4x4 matrix: result = M * v */
    mulVec4: function(m, v) {
      return [
        m[0]*v[0] + m[4]*v[1] + m[8]*v[2]  + m[12]*v[3],
        m[1]*v[0] + m[5]*v[1] + m[9]*v[2]  + m[13]*v[3],
        m[2]*v[0] + m[6]*v[1] + m[10]*v[2] + m[14]*v[3],
        m[3]*v[0] + m[7]*v[1] + m[11]*v[2] + m[15]*v[3]
      ];
    },

    /** Build a TRS matrix from position [x,y,z], euler rotation [rx,ry,rz] in degrees, scale [sx,sy,sz] */
    fromTRS: function(position, rotation, scale) {
      var px = position[0], py = position[1], pz = position[2];
      var sx = scale[0], sy = scale[1], sz = scale[2];
      var DEG2RAD = Math.PI / 180;
      var rx = rotation[0] * DEG2RAD, ry = rotation[1] * DEG2RAD, rz = rotation[2] * DEG2RAD;
      var cx = Math.cos(rx), sx_ = Math.sin(rx);
      var cy = Math.cos(ry), sy_ = Math.sin(ry);
      var cz = Math.cos(rz), sz_ = Math.sin(rz);
      // Combined rotation: R = Ry * Rx * Rz
      var r00 = cy*cz + sy_*sx_*sz_;
      var r01 = cx*sz_;
      var r02 = -sy_*cz + cy*sx_*sz_;
      var r10 = cy*-sz_ + sy_*sx_*cz;
      var r11 = cx*cz;
      var r12 = sy_*sz_ + cy*sx_*cz;
      var r20 = sy_*cx;
      var r21 = -sx_;
      var r22 = cy*cx;
      return new Float32Array([
        r00*sx, r01*sx, r02*sx, 0,
        r10*sy, r11*sy, r12*sy, 0,
        r20*sz, r21*sz, r22*sz, 0,
        px, py, pz, 1
      ]);
    },

    /** Extract translation from a 4x4 column-major matrix */
    getTranslation: function(m) { return [m[12], m[13], m[14]]; },

    /** Extract scale from a 4x4 column-major matrix */
    getScale: function(m) {
      return [
        Math.sqrt(m[0]*m[0] + m[1]*m[1] + m[2]*m[2]),
        Math.sqrt(m[4]*m[4] + m[5]*m[5] + m[6]*m[6]),
        Math.sqrt(m[8]*m[8] + m[9]*m[9] + m[10]*m[10])
      ];
    },

    /** Extract euler angles (degrees) from a 4x4 column-major matrix (Y*X*Z order) */
    getRotation: function(m) {
      var scale = Mat4.getScale(m);
      var invSx = scale[0] !== 0 ? 1/scale[0] : 0;
      var invSy = scale[1] !== 0 ? 1/scale[1] : 0;
      var invSz = scale[2] !== 0 ? 1/scale[2] : 0;
      var r20 = m[8]*invSz, r21 = m[9]*invSz, r22 = m[10]*invSz;
      var r00 = m[0]*invSx, r11 = m[5]*invSy;
      var RAD2DEG = 180 / Math.PI;
      var rx = Math.asin(-Math.max(-1, Math.min(1, r21))) * RAD2DEG;
      var ry = Math.atan2(r20, r22) * RAD2DEG;
      var rz = Math.atan2(r00 !== 0 ? m[1]*invSx : 0, r11) * RAD2DEG;
      return [rx, ry, rz];
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
      var det = m[0]*inv[0] + m[1]*inv[4] + m[2]*inv[8] + m[3]*inv[12];
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
        f/aspect, 0, 0, 0,
        0, f, 0, 0,
        0, 0, (far+near)*nf, -1,
        0, 0, 2*far*near*nf, 0
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
    /** Ray-AABB intersection (slab method). */
    rayAABB: function(origin, dir, boxMin, boxMax) {
      var tMin = -Infinity, tMax = Infinity;
      for (var i = 0; i < 3; i++) {
        if (Math.abs(dir[i]) < 1e-12) {
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
      if (tMax < 0) return null;
      var dist = tMin >= 0 ? tMin : tMax;
      return { distance: dist, point: Vec3.add(origin, Vec3.scale(dir, dist)) };
    },

    /** Ray-sphere intersection (quadratic formula). */
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
      if (t1 < 0 && t2 < 0) return null;
      var dist = t1 < 0 ? t2 : t1;
      return { distance: dist, point: Vec3.add(origin, Vec3.scale(dir, dist)) };
    },

    /** Ray-plane intersection. */
    rayPlane: function(origin, dir, planeNormal, planeD) {
      var denom = Vec3.dot(dir, planeNormal);
      if (Math.abs(denom) < 1e-8) return null;
      var t = (planeD - Vec3.dot(origin, planeNormal)) / denom;
      if (t < 0) return null;
      return { distance: t, point: Vec3.add(origin, Vec3.scale(dir, t)) };
    },

    /** AABB-AABB overlap test. */
    aabbOverlap: function(minA, maxA, minB, maxB) {
      return minA[0] <= maxB[0] && maxA[0] >= minB[0] &&
             minA[1] <= maxB[1] && maxA[1] >= minB[1] &&
             minA[2] <= maxB[2] && maxA[2] >= minB[2];
    },

    /** Sphere-sphere overlap test. */
    sphereOverlap: function(cA, rA, cB, rB) {
      return Vec3.distance(cA, cB) <= rA + rB;
    },

    /** Sphere-AABB overlap test. */
    sphereAABBOverlap: function(center, radius, boxMin, boxMax) {
      var closest = [
        Math.max(boxMin[0], Math.min(center[0], boxMax[0])),
        Math.max(boxMin[1], Math.min(center[1], boxMax[1])),
        Math.max(boxMin[2], Math.min(center[2], boxMax[2]))
      ];
      return Vec3.distance(center, closest) <= radius;
    }
  };

  // =========================================================================
  // Color utilities — parse color to [r,g,b,a] in linear space
  // =========================================================================

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
      var named = {
        'white': 'ffffff', 'black': '000000', 'red': 'ff0000', 'green': '00ff00',
        'blue': '0000ff', 'yellow': 'ffff00', 'cyan': '00ffff', 'magenta': 'ff00ff',
        'orange': 'ff8800', 'purple': '8800ff', 'pink': 'ff69b4', 'brown': '8b4513',
        'grey': '808080', 'gray': '808080', 'gold': 'ffd700', 'silver': 'c0c0c0',
        'navy': '000080', 'teal': '008080', 'olive': '808000', 'maroon': '800000',
        'coral': 'ff7f50', 'salmon': 'fa8072', 'turquoise': '40e0d0',
        'ivory': 'fffff0', 'beige': 'f5f5dc', 'tan': 'd2b48c',
        'crimson': 'dc143c', 'indigo': '4b0082', 'violet': 'ee82ee',
        'khaki': 'f0e68c', 'plum': 'dda0dd', 'sienna': 'a0522d',
        'chocolate': 'd2691e', 'tomato': 'ff6347', 'orchid': 'da70d6',
        'slate': '708090', 'charcoal': '36454f', 'midnight': '191970',
        'forest': '228b22', 'lime': '32cd32', 'sky': '87ceeb',
        'steel': '4682b4', 'royal': '4169e1', 'aqua': '00ffff',
        'amber': 'ffbf00', 'copper': 'b87333', 'bronze': 'cd7f32',
        'pearl': 'eae0c8', 'champagne': 'f7e7ce', 'emerald': '50c878',
        'ruby': 'e0115f', 'sapphire': '0f52ba', 'titanium': '878681'
      };
      var lower = value.toLowerCase().replace('#', '');
      if (named[lower]) hex = named[lower];
      if (hex.length === 3) hex = hex[0]+hex[0]+hex[1]+hex[1]+hex[2]+hex[2];
      if (hex.length >= 6) {
        var sR = parseInt(hex.substring(0, 2), 16) / 255;
        var sG = parseInt(hex.substring(2, 4), 16) / 255;
        var sB = parseInt(hex.substring(4, 6), 16) / 255;
        var a = hex.length >= 8 ? parseInt(hex.substring(6, 8), 16) / 255 : 1.0;
        // sRGB to linear conversion
        var toLinear = function(c) { return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); };
        return [toLinear(sR), toLinear(sG), toLinear(sB), a];
      }
    }
    return [0.8, 0.8, 0.8, 1.0]; // Default gray
  }

  /** Check if a value looks like a texture URL string */
  function _isTextureUrl(value) {
    return typeof value === 'string' && (
      value.endsWith('.png') || value.endsWith('.jpg') || value.endsWith('.jpeg') ||
      value.endsWith('.webp') || value.endsWith('.hdr') || value.startsWith('data:image/')
    );
  }

  // =========================================================================
  // Texture cache and loading
  // =========================================================================

  var _textureCache = {};

  /** Load and cache a texture from URL. Returns a Promise<Filament.Texture>. */
  function _loadTexture(engine, url) {
    if (_textureCache[url]) return Promise.resolve(_textureCache[url]);
    return new Promise(function(resolve, reject) {
      var img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = function() {
        try {
          var cvs = document.createElement('canvas');
          cvs.width = img.naturalWidth;
          cvs.height = img.naturalHeight;
          var ctx = cvs.getContext('2d');
          ctx.drawImage(img, 0, 0);
          var imageData = ctx.getImageData(0, 0, cvs.width, cvs.height);
          var pixels = new Uint8Array(imageData.data.buffer);
          var texture = Filament.Texture.Builder()
            .width(cvs.width).height(cvs.height).levels(1)
            .sampler(Filament.Texture$Sampler.SAMPLER_2D)
            .format(Filament.Texture$InternalFormat.RGBA8)
            .build(engine);
          var pbd = Filament.PixelBufferDescriptor(pixels, Filament.PixelDataFormat.RGBA, Filament.PixelDataType.UBYTE);
          texture.setImage(engine, 0, pbd);
          texture.generateMipmaps(engine);
          _textureCache[url] = texture;
          resolve(texture);
        } catch (e) { reject(e); }
      };
      img.onerror = function() { reject(new Error('Failed to load texture: ' + url)); };
      img.src = url;
    });
  }

  // =========================================================================
  // Environment presets (synthetic SH bands)
  // =========================================================================

  var _environmentPresets = {
    studio: {
      irradiance: [0.65,0.65,0.70, 0.10,0.10,0.12, 0.15,0.15,0.18, -0.02,-0.02,-0.01, 0.04,0.04,0.05, 0.08,0.08,0.10, 0.01,0.01,0.01, -0.02,-0.02,-0.02, 0.03,0.03,0.03],
      intensity: 35000, skyColor: [0.05, 0.06, 0.1, 1.0]
    },
    outdoor: {
      irradiance: [0.90,0.95,1.10, 0.05,0.05,0.08, 0.35,0.35,0.40, -0.05,-0.03,-0.01, 0.08,0.06,0.04, 0.15,0.12,0.08, 0.02,0.02,0.02, -0.04,-0.03,-0.02, 0.06,0.05,0.04],
      intensity: 55000, skyColor: [0.4, 0.6, 0.95, 1.0]
    },
    night: {
      irradiance: [0.10,0.12,0.20, 0.01,0.01,0.02, 0.04,0.05,0.10, -0.01,-0.01,0.00, 0.01,0.01,0.02, 0.02,0.02,0.04, 0.00,0.00,0.01, -0.01,-0.01,-0.01, 0.01,0.01,0.01],
      intensity: 8000, skyColor: [0.01, 0.01, 0.03, 1.0]
    },
    neutral: {
      irradiance: [0.80,0.80,0.80, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0],
      intensity: 30000, skyColor: [0.9, 0.9, 0.9, 1.0]
    },
    sunset: {
      irradiance: [0.85,0.55,0.35, 0.10,0.04,0.02, 0.20,0.12,0.08, -0.08,-0.02,0.04, 0.06,0.02,0.01, 0.12,0.06,0.03, 0.01,0.01,0.01, -0.03,-0.01,0.02, 0.05,0.03,0.02],
      intensity: 40000, skyColor: [0.6, 0.25, 0.1, 1.0]
    },
    dawn: {
      irradiance: [0.50,0.45,0.65, 0.05,0.03,0.06, 0.15,0.12,0.20, -0.03,-0.01,0.02, 0.04,0.03,0.05, 0.08,0.06,0.10, 0.01,0.01,0.01, -0.02,-0.01,0.01, 0.03,0.02,0.04],
      intensity: 25000, skyColor: [0.35, 0.25, 0.55, 1.0]
    }
  };

  /**
   * Generate SH irradiance from sky parameters (Preetham atmospheric approximation).
   * @param {number} hour - Solar time (0-24), 12 = noon
   * @param {number} turbidity - Atmospheric haze (1-10)
   * @param {number} groundAlbedo - Ground reflectance (0-1)
   */
  function _computeSkyFromSunPosition(hour, turbidity, groundAlbedo) {
    turbidity = turbidity || 4.0;
    groundAlbedo = groundAlbedo || 0.3;
    var solarAngle = (hour - 6.0) / 12.0 * Math.PI;
    var elevation = Math.sin(solarAngle);
    var clampedElevation = Math.max(elevation, -0.1);
    var sunAzimuth = (hour < 12 ? -1 : 1) * Math.PI * 0.3;
    var sunDir = Vec3.normalize([
      Math.cos(solarAngle) * Math.sin(sunAzimuth),
      Math.max(elevation, 0.01),
      Math.cos(solarAngle) * Math.cos(sunAzimuth)
    ]);
    var horizonFactor = 1.0 - Math.min(Math.max(elevation, 0), 1.0);
    var turbidityFade = 1.0 - (turbidity - 1.0) / 18.0;
    var sunR = 1.0;
    var sunG = Math.max(0.95 - horizonFactor * 0.35 - (turbidity - 2) * 0.02, 0.2);
    var sunB = Math.max(0.90 - horizonFactor * 0.60 - (turbidity - 2) * 0.04, 0.1);
    var dayFactor = Math.max(Math.min(elevation * 2, 1), 0);
    var skyR = (0.15 + horizonFactor * 0.45) * dayFactor;
    var skyG = (0.25 + horizonFactor * 0.20) * dayFactor;
    var skyB = (0.60 - horizonFactor * 0.30) * dayFactor;
    if (elevation < 0) {
      var nightFade = Math.max(1.0 + elevation * 5, 0);
      skyR = skyR * nightFade + 0.01 * (1 - nightFade);
      skyG = skyG * nightFade + 0.01 * (1 - nightFade);
      skyB = skyB * nightFade + 0.03 * (1 - nightFade);
    }
    var intensityBase = 60000;
    var intensityFactor = Math.pow(Math.max(clampedElevation, 0), 0.6);
    var intensity = Math.max(intensityBase * intensityFactor * turbidityFade, elevation < 0 ? 2000 : 5000);
    var gR = skyR * groundAlbedo * 0.3, gG = skyG * groundAlbedo * 0.3, gB = skyB * groundAlbedo * 0.3;
    var ambient = Math.max(dayFactor * 0.6, 0.08);
    var topBias = Math.max(clampedElevation * 0.4, 0);
    var sideBias = horizonFactor * 0.08 * dayFactor;
    return {
      irradiance: [
        skyR*0.8+gR+0.05, skyG*0.7+gG+0.05, skyB*0.9+gB+0.08,
        sideBias*sunR, sideBias*sunG*0.8, sideBias*sunB*0.6,
        topBias*0.8+ambient*0.2, topBias*0.7+ambient*0.2, topBias*0.9+ambient*0.3,
        -sunDir[0]*topBias*0.1, -sunDir[0]*topBias*0.08, -sunDir[0]*topBias*0.06,
        0.03*dayFactor, 0.02*dayFactor, 0.02*dayFactor,
        gR*2, gG*2, gB*2,
        0.01*dayFactor, 0.01*dayFactor, 0.01*dayFactor,
        -0.02*dayFactor, -0.015*dayFactor, -0.01*dayFactor,
        0.02*dayFactor, 0.015*dayFactor, 0.01*dayFactor
      ],
      intensity: intensity,
      sunDirection: [-sunDir[0], -sunDir[1], -sunDir[2]],
      sunColor: [sunR, sunG, sunB],
      skyColor: [skyR, skyG, skyB, 1.0]
    };
  }

  // =========================================================================
  // Material cache and .filamat loading (for procedural geometry)
  // =========================================================================

  var _materialCache = {};
  var _materialsBasePath = 'materials/';

  /** Resolve materials path from script tag location. */
  function _resolveMaterialsPath() {
    try {
      var scripts = document.querySelectorAll('script[src]');
      for (var i = 0; i < scripts.length; i++) {
        var src = scripts[i].getAttribute('src');
        if (src && src.indexOf('sceneview') !== -1 && src.indexOf('.js') !== -1) {
          var dir = src.substring(0, src.lastIndexOf('/') + 1);
          _materialsBasePath = dir + '../materials/';
          return;
        }
      }
    } catch (e) { /* use default */ }
  }
  _resolveMaterialsPath();

  /** Load and cache a compiled .filamat material. */
  function _loadMaterial(engine, type) {
    if (_materialCache[type]) return Promise.resolve(_materialCache[type]);
    var url = _materialsBasePath + type + '.filamat';
    return fetch(url)
      .then(function(r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.arrayBuffer();
      })
      .then(function(ab) {
        var mat = Filament.Material.Builder()
          .package(new Uint8Array(ab))
          .build(engine);
        _materialCache[type] = mat;
        return mat;
      })
      .catch(function(e) {
        console.warn('SceneView: .filamat not found for "' + type + '", using Filament default');
        try {
          var mat = Filament.Material.Builder()
            .package(Filament.getSupportedMaterial('defaultlit'))
            .build(engine);
          _materialCache[type] = mat;
          return mat;
        } catch (e2) {
          return null;
        }
      });
  }

  /** Set the base path for .filamat material files. */
  function setMaterialsPath(path) {
    _materialsBasePath = path;
    if (_materialsBasePath && !_materialsBasePath.endsWith('/')) _materialsBasePath += '/';
  }

  // =========================================================================
  // In-memory GLB generator — minimal 1x1 textured quad for text/image/video
  // =========================================================================

  var _quadGLB = null;

  /** Generate a minimal GLB containing a 1x1 quad with unlit material + texture. */
  function _buildQuadGLB() {
    if (_quadGLB) return _quadGLB;
    var positions = new Float32Array([-0.5,-0.5,0, 0.5,-0.5,0, 0.5,0.5,0, -0.5,0.5,0]);
    var normals = new Float32Array([0,0,1, 0,0,1, 0,0,1, 0,0,1]);
    var uvs = new Float32Array([0,1, 1,1, 1,0, 0,0]);
    var indices = new Uint16Array([0,1,2, 0,2,3]);
    // Build glTF JSON
    var gltf = {
      asset: { version: '2.0', generator: 'SceneView' },
      scene: 0,
      scenes: [{ nodes: [0] }],
      nodes: [{ mesh: 0 }],
      meshes: [{ primitives: [{ attributes: { POSITION: 0, NORMAL: 1, TEXCOORD_0: 2 }, indices: 3, material: 0, mode: 4 }] }],
      materials: [{ pbrMetallicRoughness: { baseColorTexture: { index: 0 }, metallicFactor: 0, roughnessFactor: 1 }, alphaMode: 'BLEND', doubleSided: true }],
      textures: [{ source: 0, sampler: 0 }],
      samplers: [{ magFilter: 9729, minFilter: 9729, wrapS: 33071, wrapT: 33071 }],
      images: [{ bufferView: 4, mimeType: 'image/png' }],
      accessors: [
        { bufferView: 0, componentType: 5126, count: 4, type: 'VEC3', max: [0.5,0.5,0], min: [-0.5,-0.5,0] },
        { bufferView: 1, componentType: 5126, count: 4, type: 'VEC3' },
        { bufferView: 2, componentType: 5126, count: 4, type: 'VEC2' },
        { bufferView: 3, componentType: 5123, count: 6, type: 'SCALAR' }
      ],
      bufferViews: [],
      buffers: [{ byteLength: 0 }]
    };
    // Create 2x2 white PNG (smallest valid PNG)
    var png = new Uint8Array([
      137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,2,0,0,0,2,8,2,0,0,0,253,212,154,115,
      0,0,0,18,73,68,65,84,120,156,99,252,255,255,63,3,16,0,0,255,255,3,0,9,24,1,25,167,110,162,169,
      0,0,0,0,73,69,78,68,174,66,96,130
    ]);
    // Pack binary data
    var bufferDatas = [
      positions.buffer, normals.buffer, uvs.buffer, indices.buffer, png.buffer
    ];
    var offset = 0;
    gltf.bufferViews = [];
    var bufLen = [positions.byteLength, normals.byteLength, uvs.byteLength, indices.byteLength, png.byteLength];
    for (var i = 0; i < bufLen.length; i++) {
      gltf.bufferViews.push({ buffer: 0, byteOffset: offset, byteLength: bufLen[i] });
      offset += bufLen[i];
      while (offset % 4 !== 0) offset++; // align
    }
    gltf.buffers[0].byteLength = offset;
    // Encode JSON
    var jsonStr = JSON.stringify(gltf);
    while (jsonStr.length % 4 !== 0) jsonStr += ' ';
    var jsonBytes = new TextEncoder().encode(jsonStr);
    // Build GLB
    var totalLength = 12 + 8 + jsonBytes.byteLength + 8 + offset;
    var glb = new ArrayBuffer(totalLength);
    var view = new DataView(glb);
    // GLB header
    view.setUint32(0, 0x46546C67, true); // magic
    view.setUint32(4, 2, true); // version
    view.setUint32(8, totalLength, true);
    // JSON chunk
    view.setUint32(12, jsonBytes.byteLength, true);
    view.setUint32(16, 0x4E4F534A, true); // JSON
    new Uint8Array(glb, 20, jsonBytes.byteLength).set(jsonBytes);
    // BIN chunk
    var binStart = 20 + jsonBytes.byteLength;
    view.setUint32(binStart, offset, true);
    view.setUint32(binStart + 4, 0x004E4942, true); // BIN
    var binData = new Uint8Array(glb, binStart + 8, offset);
    var binOffset = 0;
    for (var i = 0; i < bufferDatas.length; i++) {
      binData.set(new Uint8Array(bufferDatas[i]), binOffset);
      binOffset += bufLen[i];
      while (binOffset % 4 !== 0) binOffset++;
    }
    _quadGLB = new Uint8Array(glb);
    return _quadGLB;
  }

  // =========================================================================
  // Canvas2D text rendering utilities
  // =========================================================================

  function _nextPow2(v) { var p = 1; while (p < v) p <<= 1; return p; }

  function _roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
  }

  /**
   * Render text to a Canvas2D and return pixel data + dimensions.
   * @param {Object} opts - text, fontSize, fontFamily, color, backgroundColor, padding, maxWidth, borderRadius
   * @returns {{ pixels: Uint8Array, width: number, height: number }}
   */
  function _renderTextToCanvas(opts) {
    var text = opts.text || '';
    var fontSize = opts.fontSize || 48;
    var fontFamily = opts.fontFamily || 'Arial, sans-serif';
    var color = opts.color || 'white';
    var bgColor = opts.backgroundColor || 'rgba(0,0,0,0.75)';
    var padding = opts.padding || 20;
    var maxWidth = opts.maxWidth || 512;
    var borderRadius = opts.borderRadius || 12;

    var cvs = document.createElement('canvas');
    var ctx = cvs.getContext('2d');
    ctx.font = fontSize + 'px ' + fontFamily;

    // Word wrap
    var words = text.split(' ');
    var lines = [];
    var currentLine = '';
    for (var i = 0; i < words.length; i++) {
      var testLine = currentLine ? currentLine + ' ' + words[i] : words[i];
      if (ctx.measureText(testLine).width > maxWidth - padding * 2 && currentLine) {
        lines.push(currentLine);
        currentLine = words[i];
      } else {
        currentLine = testLine;
      }
    }
    if (currentLine) lines.push(currentLine);
    if (lines.length === 0) lines = [''];

    var lineHeight = fontSize * 1.3;
    var textWidth = 0;
    for (var i = 0; i < lines.length; i++) {
      textWidth = Math.max(textWidth, ctx.measureText(lines[i]).width);
    }

    var w = _nextPow2(Math.ceil(textWidth + padding * 2));
    var h = _nextPow2(Math.ceil(lines.length * lineHeight + padding * 2));
    cvs.width = w;
    cvs.height = h;

    ctx.clearRect(0, 0, w, h);
    if (bgColor && bgColor !== 'transparent') {
      ctx.fillStyle = bgColor;
      _roundRect(ctx, 0, 0, textWidth + padding * 2, lines.length * lineHeight + padding * 2, borderRadius);
      ctx.fill();
    }
    ctx.font = fontSize + 'px ' + fontFamily;
    ctx.fillStyle = color;
    ctx.textBaseline = 'top';
    for (var i = 0; i < lines.length; i++) {
      ctx.fillText(lines[i], padding, padding + i * lineHeight);
    }

    var imageData = ctx.getImageData(0, 0, w, h);
    return { pixels: new Uint8Array(imageData.data.buffer), width: w, height: h };
  }

  // =========================================================================
  // Chroma key (green screen removal)
  // =========================================================================

  /**
   * Apply chroma key to pixel data — removes green screen.
   * @param {Uint8Array} pixels - RGBA pixel data
   * @param {Object} opts - keyColor [r,g,b] (0-255), tolerance (0-1), softness (0-1)
   */
  function _applyChromaKey(pixels, opts) {
    var keyR = opts.keyColor ? opts.keyColor[0] : 0;
    var keyG = opts.keyColor ? opts.keyColor[1] : 255;
    var keyB = opts.keyColor ? opts.keyColor[2] : 0;
    var tolerance = opts.tolerance !== undefined ? opts.tolerance : 0.4;
    var softness = opts.softness !== undefined ? opts.softness : 0.1;
    var threshold = tolerance * 441; // sqrt(255^2 * 3)
    var softThreshold = (tolerance + softness) * 441;

    for (var i = 0; i < pixels.length; i += 4) {
      var dr = pixels[i] - keyR;
      var dg = pixels[i + 1] - keyG;
      var db = pixels[i + 2] - keyB;
      var dist = Math.sqrt(dr * dr + dg * dg + db * db);
      if (dist < threshold) {
        pixels[i + 3] = 0;
      } else if (dist < softThreshold) {
        var alpha = (dist - threshold) / (softThreshold - threshold);
        pixels[i + 3] = Math.round(alpha * pixels[i + 3]);
      }
    }
  }

  var _nodeIdCounter = 0;

  /**
   * SceneNode — base class for all nodes in the scene graph.
   *
   * Provides transform hierarchy (position, rotation, scale), parent-child
   * relationships, visibility, and Filament entity management.
   */
  class SceneNode {
    /**
     * @param {string} name - Human-readable name for lookup
     * @param {object} [options] - Initial transform and state
     * @param {number[]} [options.position=[0,0,0]] - Local position
     * @param {number[]} [options.rotation=[0,0,0]] - Local euler rotation in degrees
     * @param {number[]} [options.scale=[1,1,1]] - Local scale
     * @param {boolean} [options.visible=true] - Initial visibility
     * @param {boolean} [options.enabled=true] - Initial enabled state
     */
    constructor(name, options) {
      options = options || {};
      this._id = ++_nodeIdCounter;
      this._name = name || ('node_' + this._id);
      this._position = options.position ? options.position.slice() : [0, 0, 0];
      this._rotation = options.rotation ? options.rotation.slice() : [0, 0, 0];
      this._scale = options.scale ? options.scale.slice() : [1, 1, 1];
      this._visible = options.visible !== undefined ? options.visible : true;
      this._enabled = options.enabled !== undefined ? options.enabled : true;
      this._parent = null;
      this._children = [];
      this._destroyed = false;

      // Filament references (set by subclasses or scene)
      this._entity = null;      // Primary Filament entity (if any)
      this._entities = [];      // All Filament entities owned by this node
      this._sceneInstance = null; // Reference to SceneViewInstance

      // Cached matrices
      this._localMatrixDirty = true;
      this._worldMatrixDirty = true;
      this._localMatrix = _mat4.identity();
      this._worldMatrix = _mat4.identity();
    }

    /** @returns {string} Node name */
    get name() { return this._name; }
    set name(v) { this._name = v; }

    /** @returns {number} Unique node ID */
    get id() { return this._id; }

    /** @returns {string} Node type identifier */
    get type() { return 'node'; }

    // --- Transform properties ---

    /** @returns {number[]} Local position [x, y, z] */
    get position() { return this._position; }
    set position(v) {
      this._position[0] = v[0];
      this._position[1] = v[1];
      this._position[2] = v[2];
      this._markDirty();
    }

    /** @returns {number[]} Local euler rotation [rx, ry, rz] in degrees */
    get rotation() { return this._rotation; }
    set rotation(v) {
      this._rotation[0] = v[0];
      this._rotation[1] = v[1];
      this._rotation[2] = v[2];
      this._markDirty();
    }

    /** @returns {number[]} Local scale [sx, sy, sz] */
    get scale() { return this._scale; }
    set scale(v) {
      this._scale[0] = v[0];
      this._scale[1] = v[1];
      this._scale[2] = v[2];
      this._markDirty();
    }

    /** @returns {boolean} Whether this node is visible */
    get visible() { return this._visible; }
    set visible(v) {
      this._visible = v;
      this._applyVisibility();
    }

    /** @returns {boolean} Whether this node is enabled (processes updates) */
    get enabled() { return this._enabled; }
    set enabled(v) { this._enabled = v; }

    // --- Hierarchy ---

    /** @returns {SceneNode|null} Parent node */
    get parent() { return this._parent; }

    /** @returns {SceneNode[]} Direct children (shallow copy) */
    get children() { return this._children.slice(); }

    /**
     * Add a child node. Removes from previous parent if any.
     * @param {SceneNode} node
     * @returns {SceneNode} The added child (for chaining)
     */
    addChild(node) {
      if (node._destroyed) throw new Error('SceneView: Cannot add destroyed node');
      if (node === this) throw new Error('SceneView: Cannot add node as its own child');
      if (this._isDescendantOf(node)) throw new Error('SceneView: Cannot create circular hierarchy');

      // Remove from old parent
      if (node._parent) {
        node._parent._removeChildInternal(node);
      }

      node._parent = this;
      this._children.push(node);

      // Propagate scene instance reference
      if (this._sceneInstance) {
        node._setSceneInstance(this._sceneInstance);
      }

      node._markWorldDirty();
      return node;
    }

    /**
     * Remove a child node.
     * @param {SceneNode} node
     */
    removeChild(node) {
      this._removeChildInternal(node);
      node._parent = null;
      node._markWorldDirty();
    }

    /** Remove this node from its parent */
    removeFromParent() {
      if (this._parent) {
        this._parent.removeChild(this);
      }
    }

    // --- Lookup ---

    /**
     * Find a descendant by name (depth-first).
     * @param {string} name
     * @returns {SceneNode|null}
     */
    findByName(name) {
      if (this._name === name) return this;
      for (var i = 0; i < this._children.length; i++) {
        var found = this._children[i].findByName(name);
        if (found) return found;
      }
      return null;
    }

    /**
     * Find all descendants matching a predicate.
     * @param {function(SceneNode): boolean} predicate
     * @returns {SceneNode[]}
     */
    findAll(predicate) {
      var results = [];
      this._collectMatching(predicate, results);
      return results;
    }

    /**
     * Depth-first traversal of this node and all descendants.
     * @param {function(SceneNode, number): void} callback - Receives (node, depth)
     * @param {number} [depth=0]
     */
    traverse(callback, depth) {
      depth = depth || 0;
      callback(this, depth);
      for (var i = 0; i < this._children.length; i++) {
        this._children[i].traverse(callback, depth + 1);
      }
    }

    // --- World transform (computed from hierarchy) ---

    /** @returns {Float32Array} The local 4x4 transform matrix (column-major) */
    get localMatrix() {
      if (this._localMatrixDirty) {
        this._localMatrix = _mat4.fromTRS(this._position, this._rotation, this._scale);
        this._localMatrixDirty = false;
      }
      return this._localMatrix;
    }

    /** @returns {Float32Array} The world 4x4 transform matrix (column-major) */
    get worldMatrix() {
      if (this._worldMatrixDirty) {
        if (this._parent) {
          this._worldMatrix = _mat4.multiply(this._parent.worldMatrix, this.localMatrix);
        } else {
          this._worldMatrix = new Float32Array(this.localMatrix);
        }
        this._worldMatrixDirty = false;
      }
      return this._worldMatrix;
    }

    /** @returns {number[]} World-space position [x, y, z] */
    get worldPosition() {
      return _mat4.getTranslation(this.worldMatrix);
    }

    /** @returns {number[]} World-space euler rotation [rx, ry, rz] in degrees */
    get worldRotation() {
      return _mat4.getRotation(this.worldMatrix);
    }

    /** @returns {number[]} World-space scale [sx, sy, sz] */
    get worldScale() {
      return _mat4.getScale(this.worldMatrix);
    }

    // --- Lifecycle ---

    /**
     * Destroy this node: remove from parent, destroy all children,
     * and clean up Filament resources.
     */
    destroy() {
      if (this._destroyed) return;
      this._destroyed = true;

      // Destroy children first (copy array since it mutates)
      var childrenCopy = this._children.slice();
      for (var i = 0; i < childrenCopy.length; i++) {
        childrenCopy[i].destroy();
      }
      this._children = [];

      // Remove from parent
      if (this._parent) {
        this._parent._removeChildInternal(this);
        this._parent = null;
      }

      // Remove Filament entities from scene
      this._removeFromFilamentScene();

      // Destroy Filament entities
      this._destroyFilamentEntities();

      this._sceneInstance = null;
    }

    /**
     * Deep clone this node and its children with new Filament entities.
     * Note: model assets are shared (same glTF data), but entities are new.
     * @returns {SceneNode}
     */
    clone() {
      var cloned = this._cloneSelf();
      for (var i = 0; i < this._children.length; i++) {
        cloned.addChild(this._children[i].clone());
      }
      return cloned;
    }

    // --- Internal methods ---

    _markDirty() {
      this._localMatrixDirty = true;
      this._markWorldDirty();
    }

    _markWorldDirty() {
      this._worldMatrixDirty = true;
      for (var i = 0; i < this._children.length; i++) {
        this._children[i]._markWorldDirty();
      }
    }

    _removeChildInternal(node) {
      var idx = this._children.indexOf(node);
      if (idx !== -1) {
        this._children.splice(idx, 1);
      }
    }

    _isDescendantOf(node) {
      var current = this._parent;
      while (current) {
        if (current === node) return true;
        current = current._parent;
      }
      return false;
    }

    _collectMatching(predicate, results) {
      if (predicate(this)) results.push(this);
      for (var i = 0; i < this._children.length; i++) {
        this._children[i]._collectMatching(predicate, results);
      }
    }

    _setSceneInstance(sceneInstance) {
      this._sceneInstance = sceneInstance;
      for (var i = 0; i < this._children.length; i++) {
        this._children[i]._setSceneInstance(sceneInstance);
      }
    }

    /**
     * Apply the world transform to the Filament entity via TransformManager.
     * Called each frame for dirty nodes.
     */
    _applyTransformToFilament() {
      if (!this._entity || !this._sceneInstance) return;
      var engine = this._sceneInstance._engine;
      try {
        var tm = engine.getTransformManager();
        var inst = tm.getInstance(this._entity);
        if (inst) {
          // Filament TransformManager uses column-major Float64Array
          tm.setTransform(inst, this.worldMatrix);
        }
      } catch (e) {
        // TransformManager may not be available for all entity types
      }
    }

    _applyVisibility() {
      if (!this._sceneInstance) return;
      var scene = this._sceneInstance._scene;
      var effectiveVisible = this._isEffectivelyVisible();

      for (var i = 0; i < this._entities.length; i++) {
        try {
          if (effectiveVisible) {
            scene.addEntity(this._entities[i]);
          } else {
            scene.remove(this._entities[i]);
          }
        } catch (e) { /* entity may not be in scene */ }
      }

      // Propagate to children
      for (var c = 0; c < this._children.length; c++) {
        this._children[c]._applyVisibility();
      }
    }

    _isEffectivelyVisible() {
      if (!this._visible) return false;
      if (this._parent) return this._parent._isEffectivelyVisible();
      return true;
    }

    _addToFilamentScene() {
      if (!this._sceneInstance || !this._isEffectivelyVisible()) return;
      var scene = this._sceneInstance._scene;
      for (var i = 0; i < this._entities.length; i++) {
        try { scene.addEntity(this._entities[i]); } catch (e) { /* skip */ }
      }
    }

    _removeFromFilamentScene() {
      if (!this._sceneInstance) return;
      var scene = this._sceneInstance._scene;
      for (var i = 0; i < this._entities.length; i++) {
        try { scene.remove(this._entities[i]); } catch (e) { /* skip */ }
      }
    }

    _destroyFilamentEntities() {
      if (!this._sceneInstance) return;
      var engine = this._sceneInstance._engine;
      for (var i = 0; i < this._entities.length; i++) {
        try { engine.destroyEntity(this._entities[i]); } catch (e) { /* skip */ }
      }
      this._entities = [];
      this._entity = null;
    }

    /**
     * Clone just this node (no children). Override in subclasses.
     * @returns {SceneNode}
     */
    _cloneSelf() {
      return new SceneNode(this._name + '_clone', {
        position: this._position.slice(),
        rotation: this._rotation.slice(),
        scale: this._scale.slice(),
        visible: this._visible,
        enabled: this._enabled
      });
    }
  }


  // =========================================================================
  // ModelNode — wraps a loaded glTF/GLB model
  // =========================================================================

  /**
   * ModelNode — a scene node that displays a loaded glTF/GLB model.
   */
  class ModelNode extends SceneNode {
    /**
     * @param {string} name
     * @param {object} [options]
     * @param {string} [options.url] - Model URL to load
     */
    constructor(name, options) {
      super(name, options);
      this._url = (options && options.url) || null;
      this._asset = null;
      this._loaded = false;
      this._boundingBox = null;
    }

    get type() { return 'model'; }

    /** @returns {string|null} The model URL */
    get url() { return this._url; }

    /** @returns {boolean} Whether the model has finished loading */
    get loaded() { return this._loaded; }

    /** @returns {object|null} Bounding box {min: [x,y,z], max: [x,y,z]} */
    get boundingBox() { return this._boundingBox; }

    /**
     * Load a glTF/GLB model from URL.
     * @param {string} url
     * @returns {Promise<ModelNode>}
     */
    load(url) {
      var self = this;
      this._url = url || this._url;
      if (!this._url) return Promise.reject(new Error('ModelNode: No URL specified'));
      if (!this._sceneInstance) return Promise.reject(new Error('ModelNode: Not attached to a scene'));

      return fetch(this._url)
        .then(function(resp) {
          if (!resp.ok) throw new Error('ModelNode: HTTP ' + resp.status + ' loading ' + self._url);
          return resp.arrayBuffer();
        })
        .then(function(buffer) {
          if (self._destroyed) return self;

          Filament.assets = Filament.assets || {};
          Filament.assets[self._url] = new Uint8Array(buffer);

          var data = Filament.assets[self._url];
          var loader = self._sceneInstance._loader;
          var asset = loader.createAsset(data);
          if (!asset) throw new Error('ModelNode: Failed to parse model: ' + self._url);

          asset.loadResources();
          self._asset = asset;
          self._loaded = true;

          // Collect entities
          var root = asset.getRoot();
          var renderables = asset.getRenderableEntities();
          self._entity = root;
          self._entities = [root].concat(Array.from(renderables));

          // Add to scene
          self._addToFilamentScene();

          // Compute bounding box
          try {
            var bbox = asset.getBoundingBox();
            self._boundingBox = { min: bbox.min.slice(), max: bbox.max.slice() };
          } catch (e) { /* skip */ }

          // Apply transform
          self._applyTransformToFilament();

          return self;
        });
    }

    _removeFromFilamentScene() {
      if (!this._sceneInstance || !this._asset) return;
      var scene = this._sceneInstance._scene;
      try {
        var renderables = this._asset.getRenderableEntities();
        for (var i = 0; i < renderables.length; i++) {
          scene.remove(renderables[i]);
        }
        scene.remove(this._asset.getRoot());
      } catch (e) { /* ignore */ }
    }

    _addToFilamentScene() {
      if (!this._sceneInstance || !this._asset || !this._isEffectivelyVisible()) return;
      var scene = this._sceneInstance._scene;
      try {
        scene.addEntity(this._asset.getRoot());
        scene.addEntities(this._asset.getRenderableEntities());
      } catch (e) { /* skip */ }
    }

    _cloneSelf() {
      return new ModelNode(this._name + '_clone', {
        position: this._position.slice(),
        rotation: this._rotation.slice(),
        scale: this._scale.slice(),
        visible: this._visible,
        enabled: this._enabled,
        url: this._url
      });
    }
  }


  // =========================================================================
  // LightNode — wraps a Filament light entity
  // =========================================================================

  /** Map light type strings to Filament enum values */
  var _lightTypeMap = {
    'point': 'POINT',
    'spot': 'SPOT',
    'directional': 'DIRECTIONAL',
    'sun': 'SUN'
  };

  /**
   * LightNode — a scene node that emits light.
   */
  class LightNode extends SceneNode {
    /**
     * @param {string} name
     * @param {object} [options]
     * @param {string} [options.lightType='point'] - 'point', 'spot', 'directional', 'sun'
     * @param {number[]} [options.color=[1,1,1]] - Light color RGB
     * @param {number} [options.intensity=10000] - Light intensity (lumens for point/spot, lux for directional/sun)
     * @param {number[]} [options.direction=[0,-1,0]] - Direction for directional/sun/spot lights
     * @param {number} [options.falloffRadius=10] - Falloff radius for point/spot lights
     * @param {number} [options.spotInnerAngle=30] - Inner cone angle in degrees for spot lights
     * @param {number} [options.spotOuterAngle=45] - Outer cone angle in degrees for spot lights
     * @param {boolean} [options.castShadows=false] - Whether this light casts shadows
     */
    constructor(name, options) {
      super(name, options);
      options = options || {};
      this._lightType = options.lightType || 'point';
      this._color = options.color ? options.color.slice() : [1, 1, 1];
      this._intensity = options.intensity !== undefined ? options.intensity : 10000;
      this._direction = options.direction ? options.direction.slice() : [0, -1, 0];
      this._falloffRadius = options.falloffRadius !== undefined ? options.falloffRadius : 10;
      this._spotInnerAngle = options.spotInnerAngle !== undefined ? options.spotInnerAngle : 30;
      this._spotOuterAngle = options.spotOuterAngle !== undefined ? options.spotOuterAngle : 45;
      this._castShadows = options.castShadows || false;
    }

    get type() { return 'light'; }

    /** @returns {string} Light type */
    get lightType() { return this._lightType; }

    /** @returns {number[]} Light color RGB */
    get color() { return this._color; }
    set color(v) {
      this._color = v.slice();
      this._rebuildLight();
    }

    /** @returns {number} Light intensity */
    get intensity() { return this._intensity; }
    set intensity(v) {
      this._intensity = v;
      this._rebuildLight();
    }

    /** @returns {number[]} Light direction */
    get direction() { return this._direction; }
    set direction(v) {
      this._direction = v.slice();
      this._rebuildLight();
    }

    /** @returns {boolean} Whether this light casts shadows */
    get castShadows() { return this._castShadows; }
    set castShadows(v) {
      this._castShadows = v;
      this._rebuildLight();
    }

    /**
     * Build the Filament light entity. Called when attached to a scene.
     * @private
     */
    _buildLight() {
      if (!this._sceneInstance) return;
      var engine = this._sceneInstance._engine;

      // Destroy previous entity
      this._removeFromFilamentScene();
      this._destroyFilamentEntities();

      var entity = Filament.EntityManager.get().create();
      var typeStr = _lightTypeMap[this._lightType] || 'POINT';
      var filamentType = Filament.LightManager$Type[typeStr];
      if (!filamentType) {
        console.warn('SceneView: Unknown light type "' + this._lightType + '", falling back to POINT');
        filamentType = Filament.LightManager$Type.POINT;
      }

      var builder = Filament.LightManager.Builder(filamentType)
        .color(this._color)
        .intensity(this._intensity)
        .castShadows(this._castShadows);

      if (this._lightType === 'directional' || this._lightType === 'sun' || this._lightType === 'spot') {
        builder.direction(this._direction);
      }

      if (this._lightType === 'point' || this._lightType === 'spot') {
        builder.falloff(this._falloffRadius);
      }

      if (this._lightType === 'spot') {
        var DEG2RAD = Math.PI / 180;
        builder.spotLightCone(
          this._spotInnerAngle * DEG2RAD,
          this._spotOuterAngle * DEG2RAD
        );
      }

      if (this._lightType === 'sun') {
        builder.sunAngularRadius(1.9);
        builder.sunHaloSize(10.0);
        builder.sunHaloFalloff(80.0);
      }

      builder.build(engine, entity);

      this._entity = entity;
      this._entities = [entity];

      this._addToFilamentScene();
      this._applyTransformToFilament();
    }

    /** Rebuild light when properties change */
    _rebuildLight() {
      if (this._sceneInstance) {
        this._buildLight();
      }
    }

    _cloneSelf() {
      return new LightNode(this._name + '_clone', {
        position: this._position.slice(),
        rotation: this._rotation.slice(),
        scale: this._scale.slice(),
        visible: this._visible,
        enabled: this._enabled,
        lightType: this._lightType,
        color: this._color.slice(),
        intensity: this._intensity,
        direction: this._direction.slice(),
        falloffRadius: this._falloffRadius,
        spotInnerAngle: this._spotInnerAngle,
        spotOuterAngle: this._spotOuterAngle,
        castShadows: this._castShadows
      });
    }
  }


  // =========================================================================
  // GeometryNode — wraps procedural geometry (cube, sphere, etc.)
  // =========================================================================

  /**
   * GeometryNode — a scene node for procedural geometry shapes.
   */
  class GeometryNode extends SceneNode {
    /**
     * @param {string} name
     * @param {object} [options]
     * @param {string} [options.shape='cube'] - Shape type: 'cube', 'sphere', 'cylinder', 'plane'
     * @param {number[]} [options.size=[1,1,1]] - Size for cube, [radius] for sphere, [radius,height] for cylinder, [w,h] for plane
     * @param {number[]} [options.color=[0.8,0.8,0.8]] - Base color RGB
     * @param {number} [options.metallic=0.0] - Metallic factor (0-1)
     * @param {number} [options.roughness=0.4] - Roughness factor (0-1)
     * @param {number} [options.segments=32] - Tessellation segments (sphere, cylinder)
     */
    constructor(name, options) {
      super(name, options);
      options = options || {};
      this._shape = options.shape || 'cube';
      this._size = options.size ? options.size.slice() : [1, 1, 1];
      this._color = options.color ? options.color.slice() : [0.8, 0.8, 0.8];
      this._metallic = options.metallic !== undefined ? options.metallic : 0.0;
      this._roughness = options.roughness !== undefined ? options.roughness : 0.4;
      this._segments = options.segments || 32;
    }

    get type() { return 'geometry'; }

    /** @returns {string} Shape type */
    get shape() { return this._shape; }

    /** @returns {number[]} Base color RGB */
    get color() { return this._color; }
    set color(v) {
      this._color = v.slice();
      this._rebuildGeometry();
    }

    /**
     * Build the Filament renderable entity. Called when attached to a scene.
     * @private
     */
    _buildGeometry() {
      if (!this._sceneInstance) return;
      var engine = this._sceneInstance._engine;

      // Destroy previous
      this._removeFromFilamentScene();
      this._destroyFilamentEntities();

      var mesh;
      try {
        switch (this._shape) {
          case 'cube':
            mesh = this._createCubeMesh(engine);
            break;
          case 'sphere':
            mesh = this._createSphereMesh(engine);
            break;
          case 'cylinder':
            mesh = this._createCylinderMesh(engine);
            break;
          case 'plane':
            mesh = this._createPlaneMesh(engine);
            break;
          default:
            console.warn('SceneView: Unknown geometry shape "' + this._shape + '", using cube');
            mesh = this._createCubeMesh(engine);
        }
      } catch (e) {
        console.warn('SceneView: Failed to create geometry for "' + this._shape + '":', e.message);
        return;
      }

      if (!mesh) return;

      this._entity = mesh.entity;
      this._entities = [mesh.entity];

      this._addToFilamentScene();
      this._applyTransformToFilament();
    }

    /** Create a unit cube renderable */
    _createCubeMesh(engine) {
      var sx = this._size[0] / 2, sy = this._size[1] / 2, sz = this._size[2] / 2;

      // 24 vertices (4 per face for proper normals)
      var positions = new Float32Array([
        // Front face (z+)
        -sx, -sy,  sz,   sx, -sy,  sz,   sx,  sy,  sz,  -sx,  sy,  sz,
        // Back face (z-)
        sx, -sy, -sz,  -sx, -sy, -sz,  -sx,  sy, -sz,   sx,  sy, -sz,
        // Top face (y+)
        -sx,  sy,  sz,   sx,  sy,  sz,   sx,  sy, -sz,  -sx,  sy, -sz,
        // Bottom face (y-)
        -sx, -sy, -sz,   sx, -sy, -sz,   sx, -sy,  sz,  -sx, -sy,  sz,
        // Right face (x+)
        sx, -sy,  sz,   sx, -sy, -sz,   sx,  sy, -sz,   sx,  sy,  sz,
        // Left face (x-)
        -sx, -sy, -sz,  -sx, -sy,  sz,  -sx,  sy,  sz,  -sx,  sy, -sz
      ]);

      var normals = new Float32Array([
        0,0,1, 0,0,1, 0,0,1, 0,0,1,     // Front
        0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,  // Back
        0,1,0, 0,1,0, 0,1,0, 0,1,0,      // Top
        0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0,  // Bottom
        1,0,0, 1,0,0, 1,0,0, 1,0,0,      // Right
        -1,0,0, -1,0,0, -1,0,0, -1,0,0   // Left
      ]);

      var indices = new Uint16Array([
        0,1,2, 0,2,3,       // Front
        4,5,6, 4,6,7,       // Back
        8,9,10, 8,10,11,    // Top
        12,13,14, 12,14,15, // Bottom
        16,17,18, 16,18,19, // Right
        20,21,22, 20,22,23  // Left
      ]);

      return this._buildRenderable(engine, positions, normals, indices, 24, 36);
    }

    /** Create a UV sphere renderable */
    _createSphereMesh(engine) {
      var radius = this._size[0] || 0.5;
      var seg = this._segments;
      var rings = Math.floor(seg / 2);

      var verts = [];
      var norms = [];
      var idxArr = [];

      for (var ring = 0; ring <= rings; ring++) {
        var phi = Math.PI * ring / rings;
        var sp = Math.sin(phi), cp = Math.cos(phi);
        for (var s = 0; s <= seg; s++) {
          var theta = 2 * Math.PI * s / seg;
          var st = Math.sin(theta), ct = Math.cos(theta);
          var nx = ct * sp, ny = cp, nz = st * sp;
          verts.push(nx * radius, ny * radius, nz * radius);
          norms.push(nx, ny, nz);
        }
      }

      for (var ring = 0; ring < rings; ring++) {
        for (var s = 0; s < seg; s++) {
          var a = ring * (seg + 1) + s;
          var b = a + seg + 1;
          idxArr.push(a, b, a + 1);
          idxArr.push(a + 1, b, b + 1);
        }
      }

      return this._buildRenderable(engine,
        new Float32Array(verts), new Float32Array(norms),
        new Uint16Array(idxArr), verts.length / 3, idxArr.length);
    }

    /** Create a cylinder renderable */
    _createCylinderMesh(engine) {
      var radius = this._size[0] || 0.5;
      var height = this._size[1] || 1.0;
      var seg = this._segments;
      var halfH = height / 2;

      var verts = [];
      var norms = [];
      var idxArr = [];

      // Side vertices
      for (var i = 0; i <= seg; i++) {
        var angle = 2 * Math.PI * i / seg;
        var ca = Math.cos(angle), sa = Math.sin(angle);
        // Bottom ring
        verts.push(ca * radius, -halfH, sa * radius);
        norms.push(ca, 0, sa);
        // Top ring
        verts.push(ca * radius, halfH, sa * radius);
        norms.push(ca, 0, sa);
      }

      // Side indices
      for (var i = 0; i < seg; i++) {
        var b0 = i * 2, t0 = b0 + 1, b1 = b0 + 2, t1 = b0 + 3;
        idxArr.push(b0, b1, t0);
        idxArr.push(t0, b1, t1);
      }

      // Top and bottom caps
      var topCenter = verts.length / 3;
      verts.push(0, halfH, 0);
      norms.push(0, 1, 0);
      var botCenter = verts.length / 3;
      verts.push(0, -halfH, 0);
      norms.push(0, -1, 0);

      for (var i = 0; i <= seg; i++) {
        var angle = 2 * Math.PI * i / seg;
        var ca = Math.cos(angle), sa = Math.sin(angle);
        // Top cap vertex
        var tIdx = verts.length / 3;
        verts.push(ca * radius, halfH, sa * radius);
        norms.push(0, 1, 0);
        // Bottom cap vertex
        var bIdx = verts.length / 3;
        verts.push(ca * radius, -halfH, sa * radius);
        norms.push(0, -1, 0);

        if (i > 0) {
          idxArr.push(topCenter, tIdx - 2, tIdx);
          idxArr.push(botCenter, bIdx, bIdx - 2);
        }
      }

      return this._buildRenderable(engine,
        new Float32Array(verts), new Float32Array(norms),
        new Uint16Array(idxArr), verts.length / 3, idxArr.length);
    }

    /** Create a flat plane renderable (XZ plane, centered at origin) */
    _createPlaneMesh(engine) {
      var hw = (this._size[0] || 1) / 2;
      var hh = (this._size[1] || 1) / 2;

      var positions = new Float32Array([
        -hw, 0, -hh,
         hw, 0, -hh,
         hw, 0,  hh,
        -hw, 0,  hh
      ]);
      var normals = new Float32Array([
        0, 1, 0,
        0, 1, 0,
        0, 1, 0,
        0, 1, 0
      ]);
      var indices = new Uint16Array([0, 2, 1, 0, 3, 2]);

      return this._buildRenderable(engine, positions, normals, indices, 4, 6);
    }

    /**
     * Build a Filament renderable from raw vertex/index data.
     * Uses the default material (lit PBR via Filament's default material).
     */
    _buildRenderable(engine, positions, normals, indices, vertexCount, indexCount) {
      var entity = Filament.EntityManager.get().create();

      // Create vertex buffer
      var vb = Filament.VertexBuffer.Builder()
        .vertexCount(vertexCount)
        .bufferCount(2)
        .attribute(Filament.VertexAttribute.POSITION, 0,
          Filament.VertexBuffer$AttributeType.FLOAT3, 0, 12)
        .attribute(Filament.VertexAttribute.TANGENTS, 1,
          Filament.VertexBuffer$AttributeType.FLOAT3, 0, 12)
        .build(engine);

      vb.setBufferAt(engine, 0, positions);
      vb.setBufferAt(engine, 1, normals);

      // Create index buffer
      var ib = Filament.IndexBuffer.Builder()
        .indexCount(indexCount)
        .bufferType(Filament.IndexBuffer$IndexType.USHORT)
        .build(engine);

      ib.setBuffer(engine, indices);

      // Create default lit material
      var mat;
      try {
        mat = Filament.Material.Builder()
          .package(Filament.getSupportedMaterial('defaultlit'))
          .build(engine);
      } catch (e) {
        // Fallback: try creating a basic material
        try {
          mat = Filament.Material.Builder()
            .package(Filament.getSupportedMaterial('lit'))
            .build(engine);
        } catch (e2) {
          console.warn('SceneView: No default material available for geometry');
          return null;
        }
      }

      var matInstance = mat.createInstance();
      try {
        matInstance.setColor3Parameter('baseColor', Filament.RgbType.LINEAR, this._color);
      } catch (e) { /* param may not exist */ }
      try {
        matInstance.setFloatParameter('metallic', this._metallic);
        matInstance.setFloatParameter('roughness', this._roughness);
      } catch (e) { /* skip */ }

      // Build renderable
      Filament.RenderableManager.Builder(1)
        .boundingBox({
          center: [0, 0, 0],
          halfExtent: [this._size[0]/2, this._size[1]/2, this._size[2]/2]
        })
        .material(0, matInstance)
        .geometry(0, Filament.RenderableManager$PrimitiveType.TRIANGLES, vb, ib)
        .build(engine, entity);

      return { entity: entity, vb: vb, ib: ib, mat: mat, matInstance: matInstance };
    }

    /** Rebuild when properties change */
    _rebuildGeometry() {
      if (this._sceneInstance) {
        this._buildGeometry();
      }
    }

    _cloneSelf() {
      return new GeometryNode(this._name + '_clone', {
        position: this._position.slice(),
        rotation: this._rotation.slice(),
        scale: this._scale.slice(),
        visible: this._visible,
        enabled: this._enabled,
        shape: this._shape,
        size: this._size.slice(),
        color: this._color.slice(),
        metallic: this._metallic,
        roughness: this._roughness,
        segments: this._segments
      });
    }
  }


  // =========================================================================
  // GroupNode — empty transform node for grouping children
  // =========================================================================

  /**
   * GroupNode — an empty transform node for hierarchical grouping.
   * Has no Filament entity of its own; exists only to group children
   * under a shared transform.
   */
  class GroupNode extends SceneNode {
    constructor(name, options) {
      super(name, options);
    }

    get type() { return 'group'; }

    _cloneSelf() {
      return new GroupNode(this._name + '_clone', {
        position: this._position.slice(),
        rotation: this._rotation.slice(),
        scale: this._scale.slice(),
        visible: this._visible,
        enabled: this._enabled
      });
    }
  }


  // =========================================================================
  // SceneBuilder — fluent DSL for declarative scene construction
  // =========================================================================

  /**
   * SceneBuilder — provides a chainable, declarative API for building scenes.
   * Used inside scene.build(root => { ... }) callbacks.
   */
  class SceneBuilder {
    /**
     * @param {SceneNode} parentNode - The node to add children to
     * @param {SceneViewInstance} sceneInstance - The scene instance
     */
    constructor(parentNode, sceneInstance) {
      this._parentNode = parentNode;
      this._sceneInstance = sceneInstance;
    }

    /**
     * Add a model node.
     * @param {string} url - Model URL (.glb/.gltf)
     * @param {object} [options] - Node options (position, rotation, scale, name, etc.)
     * @returns {ModelNode}
     */
    model(url, options) {
      options = options || {};
      var name = options.name || url.split('/').pop().split('.')[0];
      var node = new ModelNode(name, options);
      node._url = url;
      this._parentNode.addChild(node);
      node._setSceneInstance(this._sceneInstance);
      // Kick off async model loading
      node.load(url).catch(function(e) {
        console.warn('SceneView: Failed to load model "' + url + '":', e.message);
      });
      return node;
    }

    /**
     * Add a light node.
     * @param {string} lightType - 'point', 'spot', 'directional', 'sun'
     * @param {object} [options] - Light and node options
     * @returns {LightNode}
     */
    light(lightType, options) {
      options = options || {};
      options.lightType = lightType;
      var name = options.name || (lightType + '_light_' + (++_nodeIdCounter));
      var node = new LightNode(name, options);
      this._parentNode.addChild(node);
      node._setSceneInstance(this._sceneInstance);
      node._buildLight();
      return node;
    }

    /**
     * Add a group node with optional builder callback for children.
     * @param {string} name - Group name
     * @param {function(SceneBuilder)|object} [optionsOrCallback] - Options or builder callback
     * @param {function(SceneBuilder)} [callback] - Builder callback if options provided
     * @returns {GroupNode}
     */
    group(name, optionsOrCallback, callback) {
      var options = {};
      var cb = null;

      if (typeof optionsOrCallback === 'function') {
        cb = optionsOrCallback;
      } else if (optionsOrCallback) {
        options = optionsOrCallback;
        cb = callback || null;
      }

      var node = new GroupNode(name, options);
      this._parentNode.addChild(node);
      node._setSceneInstance(this._sceneInstance);

      if (cb) {
        var childBuilder = new SceneBuilder(node, this._sceneInstance);
        cb(childBuilder);
      }

      return node;
    }

    /**
     * Add a cube geometry node.
     * @param {object} [options] - size, color, position, etc.
     * @returns {GeometryNode}
     */
    cube(options) {
      return this._geometry('cube', options);
    }

    /**
     * Add a sphere geometry node.
     * @param {object} [options] - size (radius), color, segments, position, etc.
     * @returns {GeometryNode}
     */
    sphere(options) {
      return this._geometry('sphere', options);
    }

    /**
     * Add a cylinder geometry node.
     * @param {object} [options] - size ([radius, height]), color, segments, position, etc.
     * @returns {GeometryNode}
     */
    cylinder(options) {
      return this._geometry('cylinder', options);
    }

    /**
     * Add a plane geometry node.
     * @param {object} [options] - size ([width, height]), color, position, etc.
     * @returns {GeometryNode}
     */
    plane(options) {
      return this._geometry('plane', options);
    }

    /**
     * Add a generic geometry node.
     * @param {string} shape
     * @param {object} [options]
     * @returns {GeometryNode}
     * @private
     */
    _geometry(shape, options) {
      options = options || {};
      options.shape = shape;
      var name = options.name || (shape + '_' + (++_nodeIdCounter));
      var node = new GeometryNode(name, options);
      this._parentNode.addChild(node);
      node._setSceneInstance(this._sceneInstance);
      node._buildGeometry();
      return node;
    }
  }


  // =========================================================================
  // SceneViewInstance — unified engine wrapper (all features merged)
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

      // Scene graph (from scene-graph branch)
      this._root = new GroupNode('root');
      this._root._setSceneInstance(this);

      // Animation system (from animation branch)
      this._animator = null;
      this._animationStates = [];
      this._animationCallbacks = { onEnd: {}, onLoop: {} };
      this._lastFrameTime = performance.now() / 1000;
      this._crossFades = [];
      this._morphAnimations = [];

      // Collision system (from collision-gestures branch)
      this._colliders = new Map();
      this._entityBounds = new Map();

      // Gesture system (from collision-gestures branch)
      this._gestureCallbacks = {
        tap: [], doubleTap: [], longPress: [],
        drag: new Map(), pinch: [], rotate: [], select: []
      };
      this._selectableEntities = new Set();
      this._selectedEntities = new Set();
      this._draggableEntities = new Map();
      this._activeDragEntity = null;
      this._activeDragPlane = null;
      this._activeDragLastWorld = null;
      this._pointers = new Map();
      this._lastTapTime = 0;
      this._lastTapPos = { x: 0, y: 0 };
      this._longPressTimer = null;
      this._longPressDuration = 500;
      this._tapDistanceThreshold = 8;
      this._doubleTapInterval = 300;
      this._gestureConsumed = false;
      this._pinchStartDist = 0;
      this._rotateStartAngle = 0;

      // Media nodes (from media-nodes branch)
      this._mediaNodes = [];
      this._billboards = [];

      // Material system (from materials-env branch)
      this._materials = new Map();
      this._materialCounter = 0;
      this._currentIbl = null;
      this._sunEntity = null;
      this._fillEntity = null;
      this._backEntity = null;
      this._environmentPreset = null;
      this._skyAnimation = null;
      this._reflectionProbes = [];
      this._reflectionProbeCounter = 0;
      this._groundPlaneEntity = null;
      this._groundPlaneMaterial = null;
      this._exposure = null;

      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

    // --- Engine/scene access ---
    get engine() { return this._engine; }
    get scene() { return this._scene; }
    get root() { return this._root; }

    // =====================================================================
    // Scene graph (from scene-graph branch)
    // =====================================================================

    /** Declarative scene builder. */
    build(callback) {
      var builder = new SceneBuilder(this._root, this);
      callback(builder);
      return this;
    }

    findByName(name) { return this._root.findByName(name); }

    clear() {
      var children = this._root.children;
      for (var i = 0; i < children.length; i++) children[i].destroy();
    }

    traverse(callback) { this._root.traverse(callback); }

    addModel(url, options) {
      options = options || {};
      var name = options.name || url.split('/').pop().split('.')[0];
      var node = new ModelNode(name, options);
      this._root.addChild(node);
      node._setSceneInstance(this);
      return node.load(url);
    }

    addLight(lightType, options) {
      options = options || {};
      options.lightType = lightType;
      var name = options.name || (lightType + '_light');
      var node = new LightNode(name, options);
      this._root.addChild(node);
      node._setSceneInstance(this);
      node._buildLight();
      return node;
    }

    addCube(options) { return this._addGeometry('cube', options); }
    addSphere(options) { return this._addGeometry('sphere', options); }
    addCylinder(options) { return this._addGeometry('cylinder', options); }
    addPlane(options) { return this._addGeometry('plane', options); }

    addGroup(name, options) {
      var node = new GroupNode(name || 'group', options);
      this._root.addChild(node);
      node._setSceneInstance(this);
      return node;
    }

    _addGeometry(shape, options) {
      options = options || {};
      options.shape = shape;
      var name = options.name || shape;
      var node = new GeometryNode(name, options);
      this._root.addChild(node);
      node._setSceneInstance(this);
      node._buildGeometry();
      return node;
    }

    // =====================================================================
    // Legacy model loading API (backward compatible)
    // =====================================================================

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
          var self = this;
          this._asset.getRenderableEntities().forEach(function(e) { self._scene.remove(e); });
          this._scene.remove(this._asset.getRoot());
        } catch (e) { /* ignore */ }
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
      this._initAnimations(asset);
      this._cacheEntityBounds();
      // Auto-frame
      try {
        var bbox = asset.getBoundingBox();
        var cx = (bbox.min[0]+bbox.max[0])/2, cy = (bbox.min[1]+bbox.max[1])/2, cz = (bbox.min[2]+bbox.max[2])/2;
        var sx = bbox.max[0]-bbox.min[0], sy = bbox.max[1]-bbox.min[1], sz = bbox.max[2]-bbox.min[2];
        var maxDim = Math.max(sx, sy, sz);
        if (maxDim > 0) {
          this._orbitTarget = [cx, cy, cz];
          this._orbitRadius = maxDim * 1.8;
          this._orbitHeight = cy;
        }
      } catch (e) { /* use defaults */ }
    }

    /** Add a model without removing existing ones */
    loadModelAdditional(url) {
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
    loadGLBBuffer(buffer) {
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
      } catch (e) { /* ignore */ }
    }

    // =====================================================================
    // Animation system (from animation branch)
    // =====================================================================

    _initAnimations(asset) {
      this._animator = null;
      this._animationStates = [];
      this._animationCallbacks = { onEnd: {}, onLoop: {} };
      this._crossFades = [];
      this._morphAnimations = [];
      try {
        var animator = asset.getAnimator();
        if (!animator) return;
        this._animator = animator;
        var count = animator.getAnimationCount();
        for (var i = 0; i < count; i++) {
          this._animationStates.push({
            name: animator.getAnimationName(i) || ('Animation_' + i),
            duration: animator.getAnimationDuration(i) || 0,
            index: i, playing: false, paused: false, time: 0, speed: 1.0, loop: false, weight: 1.0
          });
        }
      } catch (e) { /* no animations */ }
    }

    _resolveAnimationIndex(nameOrIndex) {
      if (typeof nameOrIndex === 'number') return (nameOrIndex >= 0 && nameOrIndex < this._animationStates.length) ? nameOrIndex : -1;
      for (var i = 0; i < this._animationStates.length; i++) {
        if (this._animationStates[i].name === nameOrIndex) return i;
      }
      return -1;
    }

    getAnimations() { return this._animationStates.map(function(s) { return { name: s.name, duration: s.duration, index: s.index }; }); }
    getAnimationCount() { return this._animationStates.length; }
    getAnimationName(index) { var s = this._animationStates[index]; return s ? s.name : null; }
    getAnimationDuration(index) { var s = this._animationStates[index]; return s ? s.duration : 0; }

    playAnimation(nameOrIndex, options) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) { console.warn('SceneView: Animation not found:', nameOrIndex); return this; }
      options = options || {};
      var state = this._animationStates[idx];
      var crossFadeDuration = options.crossFadeDuration || 0;
      if (crossFadeDuration > 0) {
        for (var i = 0; i < this._animationStates.length; i++) {
          if (i !== idx && this._animationStates[i].playing) {
            this._crossFades.push({ fromIndex: i, toIndex: idx, duration: crossFadeDuration, elapsed: 0, fromStartWeight: this._animationStates[i].weight, toStartWeight: 0 });
          }
        }
        state.weight = 0;
      } else { state.weight = 1.0; }
      var wasPaused = state.paused;
      state.playing = true; state.paused = false;
      state.loop = options.loop !== undefined ? options.loop : state.loop;
      state.speed = options.speed !== undefined ? options.speed : state.speed;
      if (options.startTime !== undefined) state.time = options.startTime;
      else if (!wasPaused) state.time = state.speed >= 0 ? 0 : state.duration;
      return this;
    }

    playAllAnimations(options) { for (var i = 0; i < this._animationStates.length; i++) this.playAnimation(i, options); return this; }

    pauseAnimation(nameOrIndex) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      var s = this._animationStates[idx];
      if (s.playing) { s.playing = false; s.paused = true; }
      return this;
    }

    pauseAllAnimations() { for (var i = 0; i < this._animationStates.length; i++) this.pauseAnimation(i); return this; }

    stopAnimation(nameOrIndex) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      var s = this._animationStates[idx];
      s.playing = false; s.paused = false; s.time = 0; s.weight = 1.0;
      this._crossFades = this._crossFades.filter(function(cf) { return cf.fromIndex !== idx && cf.toIndex !== idx; });
      return this;
    }

    stopAllAnimations() {
      for (var i = 0; i < this._animationStates.length; i++) {
        var s = this._animationStates[i]; s.playing = false; s.paused = false; s.time = 0; s.weight = 1.0;
      }
      this._crossFades = [];
      return this;
    }

    setAnimationSpeed(nameOrIndex, speed) { var idx = this._resolveAnimationIndex(nameOrIndex); if (idx >= 0) this._animationStates[idx].speed = speed; return this; }
    setAnimationTime(nameOrIndex, time) { var idx = this._resolveAnimationIndex(nameOrIndex); if (idx >= 0) { var s = this._animationStates[idx]; s.time = Math.max(0, Math.min(time, s.duration)); } return this; }
    isAnimationPlaying(nameOrIndex) { var idx = this._resolveAnimationIndex(nameOrIndex); return idx >= 0 ? this._animationStates[idx].playing : false; }
    getAnimationProgress(nameOrIndex) { var idx = this._resolveAnimationIndex(nameOrIndex); if (idx < 0) return 0; var s = this._animationStates[idx]; return s.duration <= 0 ? 0 : Math.max(0, Math.min(1, s.time / s.duration)); }

    getMorphTargetNames(entity) {
      if (!this._asset) return [];
      try {
        var targetEntity = entity;
        if (targetEntity === undefined || targetEntity === null) {
          var renderables = this._asset.getRenderableEntities();
          if (!renderables || renderables.length === 0) return [];
          targetEntity = renderables[0];
        }
        var rm = this._engine.getRenderableManager();
        var inst = rm.getInstance(targetEntity);
        var count = rm.getMorphTargetCountAt(inst, 0);
        var names = [];
        for (var i = 0; i < count; i++) names.push(rm.getMorphTargetNameAt(inst, 0, i) || ('morph_' + i));
        return names;
      } catch (e) { return []; }
    }

    setMorphTargetWeight(entity, name, weight) {
      if (!this._asset) return this;
      try {
        var targetEntity = entity;
        if (weight === undefined && typeof name === 'number') { weight = name; name = entity; targetEntity = null; }
        if (targetEntity === undefined || targetEntity === null) {
          var renderables = this._asset.getRenderableEntities();
          if (!renderables || renderables.length === 0) return this;
          targetEntity = renderables[0];
        }
        var rm = this._engine.getRenderableManager();
        var inst = rm.getInstance(targetEntity);
        var morphIndex = typeof name === 'number' ? name : -1;
        if (typeof name === 'string') {
          var count = rm.getMorphTargetCountAt(inst, 0);
          for (var i = 0; i < count; i++) { if (rm.getMorphTargetNameAt(inst, 0, i) === name) { morphIndex = i; break; } }
        }
        if (morphIndex < 0) return this;
        weight = Math.max(0, Math.min(1, weight));
        var totalCount = rm.getMorphTargetCountAt(inst, 0);
        var weights = new Float32Array(totalCount);
        try { var existing = rm.getMorphWeights(inst, 0); if (existing) for (var j = 0; j < totalCount; j++) weights[j] = existing[j] || 0; } catch (e) {}
        weights[morphIndex] = weight;
        rm.setMorphWeights(inst, weights, 0);
      } catch (e) { console.warn('SceneView: Failed to set morph weight:', e.message); }
      return this;
    }

    animateMorphTarget(entity, name, fromWeight, toWeight, duration) {
      var self = this;
      if (typeof entity === 'string' && typeof name === 'number') {
        duration = toWeight; toWeight = fromWeight; fromWeight = name; name = entity; entity = null;
      }
      return new Promise(function(resolve) {
        self._morphAnimations.push({ entity: entity, name: name, fromWeight: fromWeight, toWeight: toWeight, duration: duration, elapsed: 0, resolve: resolve });
      });
    }

    onAnimationEnd(nameOrIndex, callback) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      if (!this._animationCallbacks.onEnd[idx]) this._animationCallbacks.onEnd[idx] = [];
      this._animationCallbacks.onEnd[idx].push(callback);
      return this;
    }

    onAnimationLoop(nameOrIndex, callback) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      if (!this._animationCallbacks.onLoop[idx]) this._animationCallbacks.onLoop[idx] = [];
      this._animationCallbacks.onLoop[idx].push(callback);
      return this;
    }

    _updateAnimations(deltaTime) {
      this._updateMorphAnimations(deltaTime);
      if (!this._animator || this._animationStates.length === 0) return;
      var animator = this._animator;
      var hasActive = false;
      // Update cross-fades
      for (var cf = this._crossFades.length - 1; cf >= 0; cf--) {
        var fade = this._crossFades[cf];
        fade.elapsed += deltaTime;
        var t = Math.min(1, fade.elapsed / fade.duration);
        t = t * t * (3 - 2 * t); // smooth step
        if (fade.fromIndex < this._animationStates.length) this._animationStates[fade.fromIndex].weight = fade.fromStartWeight * (1 - t);
        if (fade.toIndex < this._animationStates.length) this._animationStates[fade.toIndex].weight = t;
        if (fade.elapsed >= fade.duration) {
          if (fade.fromIndex < this._animationStates.length) { this._animationStates[fade.fromIndex].playing = false; this._animationStates[fade.fromIndex].weight = 0; }
          if (fade.toIndex < this._animationStates.length) this._animationStates[fade.toIndex].weight = 1.0;
          this._crossFades.splice(cf, 1);
        }
      }
      try { animator.resetBoneMatrices(); } catch (e) {}
      for (var i = 0; i < this._animationStates.length; i++) {
        var state = this._animationStates[i];
        if (!state.playing) continue;
        hasActive = true;
        state.time += deltaTime * state.speed;
        if (state.speed >= 0 && state.time >= state.duration) {
          if (state.loop) { var lc = Math.floor(state.time / state.duration); state.time %= state.duration; this._fireAnimCallback('onLoop', i, { name: state.name, index: i, loopCount: lc }); }
          else { state.time = state.duration; state.playing = false; state.paused = false; this._fireAnimCallback('onEnd', i, { name: state.name, index: i, time: state.time }); }
        } else if (state.speed < 0 && state.time <= 0) {
          if (state.loop) { state.time = state.duration - (Math.abs(state.time) % state.duration); this._fireAnimCallback('onLoop', i, { name: state.name, index: i, loopCount: 1 }); }
          else { state.time = 0; state.playing = false; state.paused = false; this._fireAnimCallback('onEnd', i, { name: state.name, index: i, time: 0 }); }
        }
        var ct = Math.max(0, Math.min(state.time, state.duration));
        try {
          if (state.weight > 0) {
            animator.applyAnimation(i, ct);
            if (state.weight < 1.0) try { animator.applyCrossFade(i, ct, state.weight); } catch (e) {}
          }
        } catch (e) { state.playing = false; }
      }
      if (hasActive) try { animator.updateBoneMatrices(); } catch (e) {}
    }

    _fireAnimCallback(type, index, data) {
      var cbs = this._animationCallbacks[type][index];
      if (!cbs) return;
      for (var i = 0; i < cbs.length; i++) try { cbs[i](data); } catch (e) { console.error('SceneView: Animation callback error:', e); }
    }

    _updateMorphAnimations(deltaTime) {
      for (var m = this._morphAnimations.length - 1; m >= 0; m--) {
        var morph = this._morphAnimations[m];
        morph.elapsed += deltaTime;
        var progress = Math.min(1, morph.elapsed / morph.duration);
        var smoothT = progress * progress * (3 - 2 * progress);
        var w = morph.fromWeight + (morph.toWeight - morph.fromWeight) * smoothT;
        this.setMorphTargetWeight(morph.entity, morph.name, w);
        if (morph.elapsed >= morph.duration) {
          this.setMorphTargetWeight(morph.entity, morph.name, morph.toWeight);
          this._morphAnimations.splice(m, 1);
          if (morph.resolve) morph.resolve();
        }
      }
    }

    // =====================================================================
    // Collision & hit testing (from collision-gestures branch)
    // =====================================================================

    _cacheEntityBounds() {
      if (!this._asset) return;
      try {
        var entities = this._asset.getRenderableEntities();
        var rm = this._engine.getRenderableManager();
        for (var i = 0; i < entities.length; i++) {
          try {
            var inst = rm.getInstance(entities[i]);
            if (inst) {
              var aabb = rm.getAxisAlignedBoundingBox(inst);
              if (aabb) this._entityBounds.set(entities[i], { min: aabb.min.slice(), max: aabb.max.slice() });
            }
          } catch (e) {}
        }
      } catch (e) {
        try {
          var bbox = this._asset.getBoundingBox();
          this._entityBounds.set(this._asset.getRoot(), { min: bbox.min.slice(), max: bbox.max.slice() });
        } catch (e2) {}
      }
    }

    _getCameraMatrices() {
      var t = this._orbitTarget, r = this._orbitRadius, h = this._orbitHeight;
      var eye = [t[0] + Math.sin(this._angle) * r, h, t[2] + Math.cos(this._angle) * r];
      var fovRad = (this._fov || 45) * Math.PI / 180;
      var aspect = this._canvas.width / this._canvas.height;
      return { view: Mat4.lookAt(eye, t, [0,1,0]), projection: Mat4.perspective(fovRad, aspect, 0.1, 1000), eye: eye };
    }

    screenToRay(screenX, screenY) {
      var cam = this._getCameraMatrices();
      var ndcX = (screenX / this._canvas.clientWidth) * 2 - 1;
      var ndcY = 1 - (screenY / this._canvas.clientHeight) * 2;
      var vp = Mat4.multiply(cam.projection, cam.view);
      var vpInv = Mat4.invert(vp);
      if (!vpInv) return { origin: cam.eye, direction: [0, 0, -1] };
      var nearClip = Mat4.mulVec4(vpInv, [ndcX, ndcY, -1, 1]);
      var farClip = Mat4.mulVec4(vpInv, [ndcX, ndcY, 1, 1]);
      var nearWorld = [nearClip[0]/nearClip[3], nearClip[1]/nearClip[3], nearClip[2]/nearClip[3]];
      var farWorld = [farClip[0]/farClip[3], farClip[1]/farClip[3], farClip[2]/farClip[3]];
      return { origin: nearWorld, direction: Vec3.normalize(Vec3.sub(farWorld, nearWorld)) };
    }

    hitTest(screenX, screenY) { var hits = this.hitTestAll(screenX, screenY); return hits.length > 0 ? hits[0] : null; }

    hitTestAll(screenX, screenY) {
      var ray = this.screenToRay(screenX, screenY);
      var hits = [];
      this._colliders.forEach(function(collider, entityId) {
        var hit = null;
        if (collider.type === 'box') hit = Collision.rayAABB(ray.origin, ray.direction, collider.min, collider.max);
        else if (collider.type === 'sphere') hit = Collision.raySphere(ray.origin, ray.direction, collider.center, collider.radius);
        if (hit) hits.push({ entity: entityId, point: hit.point, distance: hit.distance });
      });
      var self = this;
      this._entityBounds.forEach(function(bounds, entity) {
        if (self._colliders.has(entity)) return;
        var hit = Collision.rayAABB(ray.origin, ray.direction, bounds.min, bounds.max);
        if (hit) hits.push({ entity: entity, point: hit.point, distance: hit.distance });
      });
      hits.sort(function(a, b) { return a.distance - b.distance; });
      return hits;
    }

    setCollisionBox(entity, halfExtents, center) {
      center = center || [0,0,0];
      this._colliders.set(entity, { type: 'box', entity: entity, halfExtents: halfExtents, center: center, min: Vec3.sub(center, halfExtents), max: Vec3.add(center, halfExtents) });
      return this;
    }

    setCollisionSphere(entity, radius, center) {
      center = center || [0,0,0];
      this._colliders.set(entity, { type: 'sphere', entity: entity, radius: radius, center: center });
      return this;
    }

    removeCollision(entity) { this._colliders.delete(entity); return this; }

    checkCollision(entityA, entityB) {
      var a = this._colliders.get(entityA) || this._entityBoundsAsCollider(entityA);
      var b = this._colliders.get(entityB) || this._entityBoundsAsCollider(entityB);
      if (!a || !b) return false;
      if (a.type === 'box' && b.type === 'box') return Collision.aabbOverlap(a.min, a.max, b.min, b.max);
      if (a.type === 'sphere' && b.type === 'sphere') return Collision.sphereOverlap(a.center, a.radius, b.center, b.radius);
      if (a.type === 'sphere' && b.type === 'box') return Collision.sphereAABBOverlap(a.center, a.radius, b.min, b.max);
      if (a.type === 'box' && b.type === 'sphere') return Collision.sphereAABBOverlap(b.center, b.radius, a.min, a.max);
      return false;
    }

    _entityBoundsAsCollider(entity) {
      var bounds = this._entityBounds.get(entity);
      if (!bounds) return null;
      return { type: 'box', min: bounds.min, max: bounds.max, center: Vec3.scale(Vec3.add(bounds.min, bounds.max), 0.5) };
    }

    // =====================================================================
    // Gesture system (from collision-gestures branch)
    // =====================================================================

    onTap(callback) { this._gestureCallbacks.tap.push(callback); return this; }
    onDoubleTap(callback) { this._gestureCallbacks.doubleTap.push(callback); return this; }
    onLongPress(callback, duration) { if (typeof duration === 'number' && duration > 0) this._longPressDuration = duration; this._gestureCallbacks.longPress.push(callback); return this; }
    onDrag(entity, callbacks) { this._gestureCallbacks.drag.set(entity, callbacks); return this; }
    onPinch(callback) { this._gestureCallbacks.pinch.push(callback); return this; }
    onRotate(callback) { this._gestureCallbacks.rotate.push(callback); return this; }

    enableEntityDrag(entity, options) {
      options = options || {};
      this._draggableEntities.set(entity, { axis: options.axis || 'xz', snap: options.snap || null, bounds: options.bounds || null });
      return this;
    }

    disableEntityDrag(entity) {
      this._draggableEntities.delete(entity);
      if (this._activeDragEntity === entity) { this._activeDragEntity = null; this._activeDragPlane = null; this._activeDragLastWorld = null; }
      return this;
    }

    // --- Selection system ---

    setSelectable(entity, enabled) {
      if (enabled) { this._selectableEntities.add(entity); }
      else {
        this._selectableEntities.delete(entity);
        if (this._selectedEntities.has(entity)) { this._selectedEntities.delete(entity); this._applySelectionHighlight(entity, false); this._fireSelectCallbacks(entity, false); }
      }
      return this;
    }

    onSelect(callback) { this._gestureCallbacks.select.push(callback); return this; }
    getSelectedEntities() { return Array.from(this._selectedEntities); }

    selectEntity(entity, selected) {
      selected = selected !== false;
      if (!this._selectableEntities.has(entity)) return this;
      if (selected) this._selectedEntities.add(entity); else this._selectedEntities.delete(entity);
      this._applySelectionHighlight(entity, selected);
      this._fireSelectCallbacks(entity, selected);
      return this;
    }

    _fireSelectCallbacks(entity, selected) {
      var cbs = this._gestureCallbacks.select;
      for (var i = 0; i < cbs.length; i++) try { cbs[i]({ entity: entity, selected: selected }); } catch (e) {}
    }

    _applySelectionHighlight(entity, selected) {
      try {
        var rm = this._engine.getRenderableManager();
        var inst = rm.getInstance(entity);
        if (!inst) return;
        var count = rm.getPrimitiveCount(inst);
        for (var p = 0; p < count; p++) {
          try {
            var mat = rm.getMaterialInstanceAt(inst, p);
            if (mat) mat.setColor3Parameter('emissive', Filament.RgbType.LINEAR, selected ? [0.15, 0.25, 0.5] : [0, 0, 0]);
          } catch (e) {}
        }
      } catch (e) {}
    }

    _computeDragPlane(worldPoint, axis) {
      var cam = this._getCameraMatrices();
      var viewDir = Vec3.normalize(Vec3.sub(this._orbitTarget, cam.eye));
      var normal;
      if (axis === 'xz') normal = [0,1,0];
      else if (axis === 'xy') { normal = [0,0,1]; if (Math.abs(viewDir[2]) > 0.1) normal = Vec3.normalize([0, 0, viewDir[2] > 0 ? -1 : 1]); }
      else normal = Vec3.normalize(Vec3.negate(viewDir));
      return { normal: normal, d: Vec3.dot(normal, worldPoint) };
    }

    _startEntityDrag(entity, worldPoint, sx, sy) {
      var dragOpts = this._draggableEntities.get(entity);
      this._activeDragEntity = entity;
      this._activeDragLastWorld = worldPoint;
      this._activeDragPlane = this._computeDragPlane(worldPoint, dragOpts ? dragOpts.axis : 'xz');
      var dragCb = this._gestureCallbacks.drag.get(entity);
      if (dragCb && dragCb.onStart) try { dragCb.onStart({ entity: entity, point: worldPoint, screenX: sx, screenY: sy }); } catch (err) {}
    }

    _updateEntityDrag(sx, sy) {
      if (!this._activeDragEntity || !this._activeDragPlane) return;
      var ray = this.screenToRay(sx, sy);
      var hit = Collision.rayPlane(ray.origin, ray.direction, this._activeDragPlane.normal, this._activeDragPlane.d);
      if (!hit) return;
      var newWorld = hit.point;
      var entity = this._activeDragEntity;
      var dragOpts = this._draggableEntities.get(entity);
      if (dragOpts && dragOpts.snap) { var snap = dragOpts.snap; newWorld = [Math.round(newWorld[0]/snap)*snap, Math.round(newWorld[1]/snap)*snap, Math.round(newWorld[2]/snap)*snap]; }
      if (dragOpts && dragOpts.bounds) { var b = dragOpts.bounds; newWorld = Vec3.max(b.min, Vec3.min(b.max, newWorld)); }
      var delta = Vec3.sub(newWorld, this._activeDragLastWorld);
      this._activeDragLastWorld = newWorld;
      try {
        var tm = this._engine.getTransformManager(); var inst = tm.getInstance(entity);
        if (inst) { var transform = tm.getTransform(inst); transform[12] += delta[0]; transform[13] += delta[1]; transform[14] += delta[2]; tm.setTransform(inst, transform); }
      } catch (e) {}
      var collider = this._colliders.get(entity);
      if (collider) { collider.center = Vec3.add(collider.center, delta); if (collider.type === 'box') { collider.min = Vec3.sub(collider.center, collider.halfExtents); collider.max = Vec3.add(collider.center, collider.halfExtents); } }
      var bounds = this._entityBounds.get(entity);
      if (bounds) { bounds.min = Vec3.add(bounds.min, delta); bounds.max = Vec3.add(bounds.max, delta); }
      var dragCb = this._gestureCallbacks.drag.get(entity);
      if (dragCb && dragCb.onMove) try { dragCb.onMove({ entity: entity, point: newWorld, delta: delta, screenX: sx, screenY: sy }); } catch (err) {}
    }

    _endEntityDrag(sx, sy) {
      var entity = this._activeDragEntity;
      if (entity === null) return;
      var dragCb = this._gestureCallbacks.drag.get(entity);
      if (dragCb && dragCb.onEnd) try { dragCb.onEnd({ entity: entity, point: this._activeDragLastWorld, screenX: sx, screenY: sy }); } catch (err) {}
      this._activeDragEntity = null; this._activeDragPlane = null; this._activeDragLastWorld = null;
    }

    _startLongPress(sx, sy, hit) {
      var self = this;
      this._cancelLongPress();
      if (this._gestureCallbacks.longPress.length === 0) return;
      this._longPressTimer = setTimeout(function() {
        self._longPressTimer = null;
        var actualHit = hit || self.hitTest(sx, sy);
        var cbs = self._gestureCallbacks.longPress;
        for (var i = 0; i < cbs.length; i++) try { cbs[i]({ entity: actualHit ? actualHit.entity : null, point: actualHit ? actualHit.point : null, screenX: sx, screenY: sy }); } catch (err) {}
        self._pointers.forEach(function(ptr) { ptr.moved = true; });
      }, this._longPressDuration);
    }

    _cancelLongPress() { if (this._longPressTimer) { clearTimeout(this._longPressTimer); this._longPressTimer = null; } }

    // =====================================================================
    // Media nodes — text, image, video (from media-nodes branch)
    // =====================================================================

    /**
     * Create a 3D text node.
     * @param {Object} opts - text, fontSize, fontFamily, color, backgroundColor, position, rotation, scale, width, height
     * @returns {{ entity: number, asset: Object }}
     */
    createText(opts) {
      opts = opts || {};
      var rendered = _renderTextToCanvas(opts);
      return this._createMediaNode(rendered.pixels, rendered.width, rendered.height, opts);
    }

    /**
     * Create a 3D image node from URL.
     * @param {Object} opts - url, position, rotation, scale, width, height, billboard
     * @returns {Promise<{ entity: number, asset: Object }>}
     */
    createImage(opts) {
      opts = opts || {};
      var self = this;
      return new Promise(function(resolve, reject) {
        var img = new Image();
        img.crossOrigin = 'anonymous';
        img.onload = function() {
          var w = _nextPow2(img.naturalWidth);
          var h = _nextPow2(img.naturalHeight);
          var cvs = document.createElement('canvas');
          cvs.width = w; cvs.height = h;
          var ctx = cvs.getContext('2d');
          ctx.drawImage(img, 0, 0, w, h);
          var pixels = new Uint8Array(ctx.getImageData(0, 0, w, h).data.buffer);
          resolve(self._createMediaNode(pixels, w, h, opts));
        };
        img.onerror = function() { reject(new Error('Failed to load image: ' + opts.url)); };
        img.src = opts.url;
      });
    }

    /**
     * Create a 3D video node from URL with optional chroma key.
     * @param {Object} opts - url, position, rotation, scale, chromaKey, autoplay, loop
     * @returns {Promise<{ entity: number, video: HTMLVideoElement, _updateTexture: Function }>}
     */
    createVideo(opts) {
      opts = opts || {};
      var self = this;
      return new Promise(function(resolve, reject) {
        var video = document.createElement('video');
        video.crossOrigin = 'anonymous';
        video.playsInline = true;
        video.muted = opts.muted !== false;
        video.loop = opts.loop !== false;
        video.src = opts.url;
        video.addEventListener('loadeddata', function() {
          var w = _nextPow2(video.videoWidth);
          var h = _nextPow2(video.videoHeight);
          var cvs = document.createElement('canvas');
          cvs.width = w; cvs.height = h;
          var ctx = cvs.getContext('2d');
          ctx.drawImage(video, 0, 0, w, h);
          var imageData = ctx.getImageData(0, 0, w, h);
          var pixels = new Uint8Array(imageData.data.buffer);
          var result = self._createMediaNode(pixels, w, h, opts);
          result.video = video;
          result._canvas = cvs;
          result._ctx = ctx;
          // Update function for video frames
          result._updateTexture = function() {
            if (video.paused || video.ended) return;
            ctx.drawImage(video, 0, 0, w, h);
            var imgData = ctx.getImageData(0, 0, w, h);
            var pix = new Uint8Array(imgData.data.buffer);
            if (opts.chromaKey) _applyChromaKey(pix, opts.chromaKey);
            self._updateMediaTexture(result, pix, w, h);
          };
          self._mediaNodes.push(result);
          if (opts.autoplay !== false) video.play().catch(function() {});
          resolve(result);
        });
        video.addEventListener('error', function() { reject(new Error('Failed to load video: ' + opts.url)); });
      });
    }

    /** Internal: create a media node from pixel data by loading a GLB quad. */
    _createMediaNode(pixels, texW, texH, opts) {
      var glbData = _buildQuadGLB();
      var asset = this._loader.createAsset(glbData);
      if (!asset) return null;
      asset.loadResources();
      var root = asset.getRoot();
      var renderables = asset.getRenderableEntities();
      this._scene.addEntity(root);
      this._scene.addEntities(renderables);
      // Update texture
      if (renderables.length > 0) {
        try {
          var rm = this._engine.getRenderableManager();
          var ri = rm.getInstance(renderables[0]);
          var mi = rm.getMaterialInstanceAt(ri, 0);
          var texture = Filament.Texture.Builder()
            .width(texW).height(texH).levels(1)
            .sampler(Filament.Texture$Sampler.SAMPLER_2D)
            .format(Filament.Texture$InternalFormat.RGBA8)
            .build(this._engine);
          var pbd = Filament.PixelBufferDescriptor(pixels, Filament.PixelDataFormat.RGBA, Filament.PixelDataType.UBYTE);
          texture.setImage(this._engine, 0, pbd);
          var sampler = new Filament.TextureSampler(Filament.MinFilter.LINEAR, Filament.MagFilter.LINEAR, Filament.WrapMode.CLAMP_TO_EDGE);
          mi.setTextureParameter('baseColorMap', texture, sampler);
        } catch (e) { console.warn('SceneView: Failed to set media texture:', e.message); }
      }
      // Apply transform
      var w = opts.width || 1;
      var h = opts.height || (texH / texW);
      var pos = opts.position || [0, 0, 0];
      var rot = opts.rotation || [0, 0, 0];
      var scl = opts.scale || [w, h, 1];
      try {
        var tm = this._engine.getTransformManager();
        var inst = tm.getInstance(root);
        if (inst) tm.setTransform(inst, Mat4.fromTRS(pos, rot, scl));
      } catch (e) {}
      var result = { entity: root, asset: asset, _texture: null, _renderables: renderables };
      if (opts.billboard) { this._billboards.push(result); }
      return result;
    }

    /** Update texture for a media node (used by video frames). */
    _updateMediaTexture(mediaNode, pixels, w, h) {
      if (!mediaNode || !mediaNode._renderables || mediaNode._renderables.length === 0) return;
      try {
        var rm = this._engine.getRenderableManager();
        var ri = rm.getInstance(mediaNode._renderables[0]);
        var mi = rm.getMaterialInstanceAt(ri, 0);
        var texture = Filament.Texture.Builder()
          .width(w).height(h).levels(1)
          .sampler(Filament.Texture$Sampler.SAMPLER_2D)
          .format(Filament.Texture$InternalFormat.RGBA8)
          .build(this._engine);
        var pbd = Filament.PixelBufferDescriptor(pixels, Filament.PixelDataFormat.RGBA, Filament.PixelDataType.UBYTE);
        texture.setImage(this._engine, 0, pbd);
        var sampler = new Filament.TextureSampler(Filament.MinFilter.LINEAR, Filament.MagFilter.LINEAR, Filament.WrapMode.CLAMP_TO_EDGE);
        mi.setTextureParameter('baseColorMap', texture, sampler);
      } catch (e) {}
    }

    /** Remove a media/geometry node from the scene. */
    removeNode(nodeOrEntity) {
      var entity = typeof nodeOrEntity === 'number' ? nodeOrEntity : (nodeOrEntity && nodeOrEntity.entity);
      if (!entity) return;
      try { this._scene.remove(entity); } catch (e) {}
      this._billboards = this._billboards.filter(function(b) { return b.entity !== entity; });
      this._mediaNodes = this._mediaNodes.filter(function(m) { return m.entity !== entity; });
    }

    // =====================================================================
    // PBR Material system (from materials-env branch)
    // =====================================================================

    createMaterial(options) {
      options = options || {};
      var id = ++this._materialCounter;
      var self = this;
      var texturePromises = [];
      var textures = {};
      if (_isTextureUrl(options.baseColor)) texturePromises.push(_loadTexture(self._engine, options.baseColor).then(function(tex) { textures.baseColorMap = tex; }));
      if (_isTextureUrl(options.metallic)) texturePromises.push(_loadTexture(self._engine, options.metallic).then(function(tex) { textures.metallicMap = tex; }));
      if (_isTextureUrl(options.roughness)) texturePromises.push(_loadTexture(self._engine, options.roughness).then(function(tex) { textures.roughnessMap = tex; }));
      if (options.normal && _isTextureUrl(options.normal)) texturePromises.push(_loadTexture(self._engine, options.normal).then(function(tex) { textures.normalMap = tex; }));
      if (options.occlusion && _isTextureUrl(options.occlusion)) texturePromises.push(_loadTexture(self._engine, options.occlusion).then(function(tex) { textures.occlusionMap = tex; }));
      if (_isTextureUrl(options.emissive)) texturePromises.push(_loadTexture(self._engine, options.emissive).then(function(tex) { textures.emissiveMap = tex; }));
      var material = { id: id, options: options, _textures: textures, ready: texturePromises.length > 0 ? Promise.all(texturePromises).then(function() { return material; }) : Promise.resolve(material) };
      this._materials.set(id, material);
      return material;
    }

    setMaterial(entity, material) {
      var self = this;
      var opts = material.options;
      material.ready.then(function() {
        try {
          var rm = self._engine.getRenderableManager();
          var ri = rm.getInstance(entity);
          if (!ri) return;
          var mi = rm.getMaterialInstanceAt(ri, 0);
          if (!mi) return;
          // Base color
          if (opts.baseColor && !_isTextureUrl(opts.baseColor)) {
            var bc = _parseColor(opts.baseColor);
            if (opts.opacity !== undefined && opts.opacity < 1.0) bc[3] = opts.opacity;
            try { mi.setColor4Parameter('baseColorFactor', Filament.RgbaType.sRGB, bc); } catch(e) { try { mi.setFloat4Parameter('baseColorFactor', bc); } catch(e2) {} }
          }
          if (material._textures.baseColorMap) { try { var s = new Filament.TextureSampler(Filament.MinFilter.LINEAR_MIPMAP_LINEAR, Filament.MagFilter.LINEAR, Filament.WrapMode.REPEAT); mi.setTextureParameter('baseColorMap', material._textures.baseColorMap, s); } catch(e) {} }
          if (typeof opts.metallic === 'number') try { mi.setFloatParameter('metallicFactor', opts.metallic); } catch(e) {}
          if (typeof opts.roughness === 'number') try { mi.setFloatParameter('roughnessFactor', opts.roughness); } catch(e) {}
          if (material._textures.normalMap) { try { var s = new Filament.TextureSampler(Filament.MinFilter.LINEAR_MIPMAP_LINEAR, Filament.MagFilter.LINEAR, Filament.WrapMode.REPEAT); mi.setTextureParameter('normalMap', material._textures.normalMap, s); } catch(e) {} }
          if (opts.emissive && !_isTextureUrl(opts.emissive)) { var ec = _parseColor(opts.emissive); var str = opts.emissiveStrength || 1.0; try { mi.setColor3Parameter('emissiveFactor', Filament.RgbType.LINEAR, [ec[0]*str, ec[1]*str, ec[2]*str]); } catch(e) {} }
          if (typeof opts.transmission === 'number') try { mi.setFloatParameter('transmissionFactor', opts.transmission); } catch(e) {}
          if (typeof opts.ior === 'number') try { mi.setFloatParameter('ior', opts.ior); } catch(e) {}
          if (typeof opts.clearCoat === 'number') try { mi.setFloatParameter('clearCoatFactor', opts.clearCoat); } catch(e) {}
          if (typeof opts.clearCoatRoughness === 'number') try { mi.setFloatParameter('clearCoatRoughnessFactor', opts.clearCoatRoughness); } catch(e) {}
          if (opts.sheenColor) { var sc = _parseColor(opts.sheenColor); try { mi.setColor3Parameter('sheenColorFactor', Filament.RgbType.LINEAR, sc); } catch(e) {} }
          if (typeof opts.sheenRoughness === 'number') try { mi.setFloatParameter('sheenRoughnessFactor', opts.sheenRoughness); } catch(e) {}
        } catch (e) { console.warn('SceneView: Failed to set material:', e.message); }
      });
    }

    getMaterial(entity) {
      try {
        var rm = this._engine.getRenderableManager();
        var ri = rm.getInstance(entity);
        if (!ri) return null;
        return rm.getMaterialInstanceAt(ri, 0);
      } catch (e) { return null; }
    }

    // =====================================================================
    // Environment system (from materials-env branch)
    // =====================================================================

    setEnvironment(presetName) {
      var preset = _environmentPresets[presetName];
      if (!preset) { console.warn('SceneView: Unknown environment preset:', presetName); return this; }
      this._environmentPreset = presetName;
      this._applyIblFromSH(preset.irradiance, preset.intensity);
      this.setBackgroundColor(preset.skyColor[0], preset.skyColor[1], preset.skyColor[2], preset.skyColor[3]);
      return this;
    }

    _applyIblFromSH(irradiance, intensity) {
      try {
        if (this._currentIbl) { try { this._engine.destroyIndirectLight(this._currentIbl); } catch (e) {} }
        var ibl = Filament.IndirectLight.Builder()
          .irradiance(3, irradiance)
          .intensity(intensity)
          .build(this._engine);
        this._scene.setIndirectLight(ibl);
        this._currentIbl = ibl;
      } catch (e) { console.warn('SceneView: Failed to apply IBL:', e.message); }
    }

    setDynamicSky(opts) {
      opts = opts || {};
      var hour = opts.hour !== undefined ? opts.hour : 12;
      var turbidity = opts.turbidity || 4;
      var groundAlbedo = opts.groundAlbedo || 0.3;
      var sky = _computeSkyFromSunPosition(hour, turbidity, groundAlbedo);
      this._applyIblFromSH(sky.irradiance, sky.intensity);
      this.setBackgroundColor(sky.skyColor[0], sky.skyColor[1], sky.skyColor[2], sky.skyColor[3]);
      // Update sun direction
      if (this._sunEntity) {
        try {
          var lm = this._engine.getLightManager();
          var inst = lm.getInstance(this._sunEntity);
          if (inst) { lm.setDirection(inst, sky.sunDirection); lm.setColor(inst, sky.sunColor); }
        } catch (e) {}
      }
      return this;
    }

    animateSky(opts) {
      var self = this;
      opts = opts || {};
      var fromHour = opts.from !== undefined ? opts.from : 6;
      var toHour = opts.to !== undefined ? opts.to : 18;
      var duration = opts.duration || 10;
      var turbidity = opts.turbidity || 4;
      var groundAlbedo = opts.groundAlbedo || 0.3;
      if (this._skyAnimation) this._skyAnimation.cancelled = true;
      var anim = { cancelled: false, startTime: performance.now() };
      this._skyAnimation = anim;
      function tick() {
        if (anim.cancelled) return;
        var elapsed = (performance.now() - anim.startTime) / 1000;
        var t = Math.min(1, elapsed / duration);
        var hour = fromHour + (toHour - fromHour) * t;
        self.setDynamicSky({ hour: hour, turbidity: turbidity, groundAlbedo: groundAlbedo });
        if (t < 1) requestAnimationFrame(tick);
        else self._skyAnimation = null;
      }
      requestAnimationFrame(tick);
      return this;
    }

    setExposure(opts) {
      opts = opts || {};
      try {
        if (opts.aperture !== undefined || opts.shutterSpeed !== undefined || opts.sensitivity !== undefined) {
          this._camera.setExposure(opts.aperture || 16.0, opts.shutterSpeed || (1.0/125.0), opts.sensitivity || 100);
        }
      } catch (e) {}
      this._exposure = opts;
      return this;
    }

    setToneMapping(type) {
      try {
        var tmType = Filament.View$ToneMapping[type.toUpperCase()];
        if (tmType !== undefined) this._view.setToneMapping(tmType);
      } catch (e) {}
      return this;
    }

    // =====================================================================
    // Procedural geometry (from geometry branch)
    // =====================================================================

    createCube(opts) { return this._createGeometryEntity('cube', opts); }
    createSphere(opts) { return this._createGeometryEntity('sphere', opts); }
    createCylinder(opts) { return this._createGeometryEntity('cylinder', opts); }
    createCone(opts) { return this._createGeometryEntity('cone', opts); }
    createPlaneEntity(opts) { return this._createGeometryEntity('plane', opts); }

    _createGeometryEntity(shape, opts) {
      opts = opts || {};
      var color = _parseColor(opts.color || [0.8, 0.8, 0.8]);
      var gen;
      switch (shape) {
        case 'cube': gen = this._genCube(opts); break;
        case 'sphere': gen = this._genSphere(opts); break;
        case 'cylinder': gen = this._genCylinder(opts); break;
        case 'cone': gen = this._genCone(opts); break;
        case 'plane': gen = this._genPlane(opts); break;
        default: gen = this._genCube(opts);
      }
      if (!gen) return null;
      return this._buildGeometryEntity(gen.positions, gen.normals, gen.indices, gen.uvs, color, opts);
    }

    _genCube(opts) {
      var s = opts.size || [1,1,1]; var sx = s[0]/2, sy = s[1]/2, sz = s[2]/2;
      var p = new Float32Array([-sx,-sy,sz, sx,-sy,sz, sx,sy,sz, -sx,sy,sz, sx,-sy,-sz, -sx,-sy,-sz, -sx,sy,-sz, sx,sy,-sz, -sx,sy,sz, sx,sy,sz, sx,sy,-sz, -sx,sy,-sz, -sx,-sy,-sz, sx,-sy,-sz, sx,-sy,sz, -sx,-sy,sz, sx,-sy,sz, sx,-sy,-sz, sx,sy,-sz, sx,sy,sz, -sx,-sy,-sz, -sx,-sy,sz, -sx,sy,sz, -sx,sy,-sz]);
      var n = new Float32Array([0,0,1, 0,0,1, 0,0,1, 0,0,1, 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1, 0,1,0, 0,1,0, 0,1,0, 0,1,0, 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0, 1,0,0, 1,0,0, 1,0,0, 1,0,0, -1,0,0, -1,0,0, -1,0,0, -1,0,0]);
      var idx = new Uint16Array([0,1,2, 0,2,3, 4,5,6, 4,6,7, 8,9,10, 8,10,11, 12,13,14, 12,14,15, 16,17,18, 16,18,19, 20,21,22, 20,22,23]);
      return { positions: p, normals: n, indices: idx, uvs: null };
    }

    _genSphere(opts) {
      var radius = opts.radius || 0.5; var seg = opts.segments || 32; var rings = Math.floor(seg / 2);
      var verts = [], norms = [], idxArr = [];
      for (var ring = 0; ring <= rings; ring++) {
        var phi = Math.PI * ring / rings; var sp = Math.sin(phi), cp = Math.cos(phi);
        for (var s = 0; s <= seg; s++) {
          var theta = 2 * Math.PI * s / seg; var st = Math.sin(theta), ct = Math.cos(theta);
          var nx = ct * sp, ny = cp, nz = st * sp;
          verts.push(nx * radius, ny * radius, nz * radius); norms.push(nx, ny, nz);
        }
      }
      for (var ring = 0; ring < rings; ring++) {
        for (var s = 0; s < seg; s++) {
          var a = ring * (seg + 1) + s; var b = a + seg + 1;
          idxArr.push(a, b, a + 1); idxArr.push(a + 1, b, b + 1);
        }
      }
      return { positions: new Float32Array(verts), normals: new Float32Array(norms), indices: new Uint16Array(idxArr), uvs: null };
    }

    _genCylinder(opts) {
      var radius = opts.radius || 0.5; var height = opts.height || 1; var seg = opts.segments || 32; var halfH = height / 2;
      var verts = [], norms = [], idxArr = [];
      for (var i = 0; i <= seg; i++) { var a = 2*Math.PI*i/seg; var ca = Math.cos(a), sa = Math.sin(a); verts.push(ca*radius, -halfH, sa*radius); norms.push(ca, 0, sa); verts.push(ca*radius, halfH, sa*radius); norms.push(ca, 0, sa); }
      for (var i = 0; i < seg; i++) { var b0 = i*2, t0 = b0+1, b1 = b0+2, t1 = b0+3; idxArr.push(b0, b1, t0); idxArr.push(t0, b1, t1); }
      var topC = verts.length / 3; verts.push(0, halfH, 0); norms.push(0, 1, 0);
      var botC = verts.length / 3; verts.push(0, -halfH, 0); norms.push(0, -1, 0);
      for (var i = 0; i <= seg; i++) { var a = 2*Math.PI*i/seg; var ca = Math.cos(a), sa = Math.sin(a); var tI = verts.length/3; verts.push(ca*radius, halfH, sa*radius); norms.push(0,1,0); var bI = verts.length/3; verts.push(ca*radius, -halfH, sa*radius); norms.push(0,-1,0); if (i > 0) { idxArr.push(topC, tI-2, tI); idxArr.push(botC, bI, bI-2); } }
      return { positions: new Float32Array(verts), normals: new Float32Array(norms), indices: new Uint16Array(idxArr), uvs: null };
    }

    _genCone(opts) {
      var radius = opts.radius || 0.5; var height = opts.height || 1; var seg = opts.segments || 32; var halfH = height / 2;
      var verts = [], norms = [], idxArr = [];
      var tipIdx = 0; verts.push(0, halfH, 0); norms.push(0, 1, 0);
      var slopeLen = Math.sqrt(radius * radius + height * height);
      var ny = radius / slopeLen, nr = height / slopeLen;
      for (var i = 0; i <= seg; i++) { var a = 2*Math.PI*i/seg; var ca = Math.cos(a), sa = Math.sin(a); verts.push(ca*radius, -halfH, sa*radius); norms.push(ca*nr, ny, sa*nr); }
      for (var i = 0; i < seg; i++) idxArr.push(tipIdx, i+1, i+2);
      var botC = verts.length / 3; verts.push(0, -halfH, 0); norms.push(0, -1, 0);
      for (var i = 0; i <= seg; i++) { var a = 2*Math.PI*i/seg; var ca = Math.cos(a), sa = Math.sin(a); var bI = verts.length/3; verts.push(ca*radius, -halfH, sa*radius); norms.push(0,-1,0); if (i > 0) idxArr.push(botC, bI, bI-1); }
      return { positions: new Float32Array(verts), normals: new Float32Array(norms), indices: new Uint16Array(idxArr), uvs: null };
    }

    _genPlane(opts) {
      var s = opts.size || [1, 1]; var hw = s[0]/2, hh = s[1]/2;
      var p = new Float32Array([-hw,0,-hh, hw,0,-hh, hw,0,hh, -hw,0,hh]);
      var n = new Float32Array([0,1,0, 0,1,0, 0,1,0, 0,1,0]);
      var idx = new Uint16Array([0,2,1, 0,3,2]);
      return { positions: p, normals: n, indices: idx, uvs: null };
    }

    _buildGeometryEntity(positions, normals, indices, uvs, color, opts) {
      var engine = this._engine;
      var entity = Filament.EntityManager.get().create();
      var vertexCount = positions.length / 3;
      var indexCount = indices.length;
      var vb = Filament.VertexBuffer.Builder()
        .vertexCount(vertexCount)
        .bufferCount(2)
        .attribute(Filament.VertexAttribute.POSITION, 0, Filament.VertexBuffer$AttributeType.FLOAT3, 0, 12)
        .attribute(Filament.VertexAttribute.TANGENTS, 1, Filament.VertexBuffer$AttributeType.FLOAT3, 0, 12)
        .build(engine);
      vb.setBufferAt(engine, 0, positions);
      vb.setBufferAt(engine, 1, normals);
      var ib = Filament.IndexBuffer.Builder()
        .indexCount(indexCount)
        .bufferType(Filament.IndexBuffer$IndexType.USHORT)
        .build(engine);
      ib.setBuffer(engine, indices);
      var mat;
      try { mat = Filament.Material.Builder().package(Filament.getSupportedMaterial('defaultlit')).build(engine); }
      catch (e) { try { mat = Filament.Material.Builder().package(Filament.getSupportedMaterial('lit')).build(engine); } catch (e2) { return null; } }
      var matInstance = mat.createInstance();
      try { matInstance.setColor3Parameter('baseColor', Filament.RgbType.LINEAR, [color[0], color[1], color[2]]); } catch (e) {}
      try { matInstance.setFloatParameter('metallic', opts.metallic || 0); matInstance.setFloatParameter('roughness', opts.roughness !== undefined ? opts.roughness : 0.4); } catch (e) {}
      Filament.RenderableManager.Builder(1)
        .boundingBox({ center: [0,0,0], halfExtent: [1,1,1] })
        .material(0, matInstance)
        .geometry(0, Filament.RenderableManager$PrimitiveType.TRIANGLES, vb, ib)
        .build(engine, entity);
      this._scene.addEntity(entity);
      // Apply transform
      var pos = opts.position || [0,0,0]; var rot = opts.rotation || [0,0,0]; var scl = opts.scale || [1,1,1];
      try { var tm = engine.getTransformManager(); var inst = tm.getInstance(entity); if (inst) tm.setTransform(inst, Mat4.fromTRS(pos, rot, scl)); } catch (e) {}
      return entity;
    }

    removeEntity(entity) {
      if (!entity) return;
      try { this._scene.remove(entity); } catch (e) {}
      try { this._engine.destroyEntity(entity); } catch (e) {}
    }

    // =====================================================================
    // Basic controls
    // =====================================================================

    setAutoRotate(enabled) { this._autoRotate = enabled; this._wantsAutoRotate = enabled; return this; }
    setCameraDistance(d) { this._orbitRadius = d; return this; }
    setBackgroundColor(r, g, b, a) { this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true }); return this; }

    dispose() {
      this._running = false;
      if (this._skyAnimation) { this._skyAnimation.cancelled = true; this._skyAnimation = null; }
      if (this._resizeObserver) this._resizeObserver.disconnect();
      this.clear();
      this._colliders.clear();
      this._entityBounds.clear();
      this._selectableEntities.clear();
      this._selectedEntities.clear();
      this._draggableEntities.clear();
      this._pointers.clear();
      try { Filament.Engine.destroy(this._engine); } catch (e) {}
    }

    // =====================================================================
    // Input handling — unified pointer events with gesture detection
    // =====================================================================

    _setupControls() {
      var canvas = this._canvas;
      var self = this;
      canvas.style.touchAction = 'none';

      canvas.addEventListener('pointerdown', function(e) {
        canvas.setPointerCapture(e.pointerId);
        var rect = canvas.getBoundingClientRect();
        var sx = e.clientX - rect.left, sy = e.clientY - rect.top;
        self._pointers.set(e.pointerId, { x: sx, y: sy, startX: sx, startY: sy, startTime: Date.now(), moved: false });
        if (self._pointers.size === 2) {
          var pts = Array.from(self._pointers.values());
          self._pinchStartDist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
          self._rotateStartAngle = Math.atan2(pts[1].y - pts[0].y, pts[1].x - pts[0].x);
          self._gestureConsumed = true; self._cancelLongPress(); return;
        }
        self._gestureConsumed = false;
        var hit = self.hitTest(sx, sy);
        if (hit && self._draggableEntities.has(hit.entity)) { self._gestureConsumed = true; self._startEntityDrag(hit.entity, hit.point, sx, sy); return; }
        if (hit && self._gestureCallbacks.drag.has(hit.entity)) {
          self._gestureConsumed = true; self._activeDragEntity = hit.entity; self._activeDragLastWorld = hit.point;
          self._activeDragPlane = self._computeDragPlane(hit.point, 'xz');
          var dragCb = self._gestureCallbacks.drag.get(hit.entity);
          if (dragCb.onStart) try { dragCb.onStart({ entity: hit.entity, point: hit.point, screenX: sx, screenY: sy }); } catch (err) {}
          return;
        }
        self._startLongPress(sx, sy, hit);
        if (!self._gestureConsumed) {
          self._isDragging = true; self._lastMouse = { x: e.clientX, y: e.clientY };
          self._autoRotate = false; self._velocityAngle = 0; self._velocityHeight = 0;
          if (self._autoRotateTimer) { clearTimeout(self._autoRotateTimer); self._autoRotateTimer = null; }
        }
      });

      canvas.addEventListener('pointermove', function(e) {
        var rect = canvas.getBoundingClientRect();
        var sx = e.clientX - rect.left, sy = e.clientY - rect.top;
        var ptr = self._pointers.get(e.pointerId);
        if (ptr) {
          ptr.x = sx; ptr.y = sy;
          if (Math.hypot(sx - ptr.startX, sy - ptr.startY) > self._tapDistanceThreshold) { ptr.moved = true; self._cancelLongPress(); }
        }
        if (self._pointers.size === 2) {
          var pts = Array.from(self._pointers.values());
          var dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
          var angle = Math.atan2(pts[1].y - pts[0].y, pts[1].x - pts[0].x);
          var cx = (pts[0].x + pts[1].x) / 2, cy = (pts[0].y + pts[1].y) / 2;
          if (self._pinchStartDist > 0) {
            var scale = dist / self._pinchStartDist;
            var pinchCbs = self._gestureCallbacks.pinch;
            for (var pi = 0; pi < pinchCbs.length; pi++) try { pinchCbs[pi]({ scale: scale, centerX: cx, centerY: cy }); } catch (err) {}
            if (pinchCbs.length === 0) { self._orbitRadius /= (scale > 1 ? 1.01 : 0.99); self._orbitRadius = Math.max(0.5, Math.min(50, self._orbitRadius)); }
          }
          var angleDelta = angle - self._rotateStartAngle;
          var rotateCbs = self._gestureCallbacks.rotate;
          for (var ri = 0; ri < rotateCbs.length; ri++) try { rotateCbs[ri]({ angle: angleDelta, centerX: cx, centerY: cy }); } catch (err) {}
          return;
        }
        if (self._activeDragEntity !== null && self._activeDragPlane) { self._updateEntityDrag(sx, sy); return; }
        if (self._isDragging && !self._gestureConsumed) {
          var dx = (e.clientX - self._lastMouse.x) * 0.005, dy = (e.clientY - self._lastMouse.y) * 0.01;
          self._velocityAngle = -dx; self._velocityHeight = dy;
          self._angle -= dx; self._orbitHeight += dy;
          self._lastMouse = { x: e.clientX, y: e.clientY };
        }
      });

      canvas.addEventListener('pointerup', function(e) {
        canvas.releasePointerCapture(e.pointerId);
        var ptr = self._pointers.get(e.pointerId);
        self._pointers.delete(e.pointerId);
        if (self._activeDragEntity !== null) self._endEntityDrag(ptr ? ptr.x : 0, ptr ? ptr.y : 0);
        if (ptr && !ptr.moved && self._pointers.size === 0) {
          var elapsed = Date.now() - ptr.startTime;
          var sx = ptr.startX, sy = ptr.startY;
          self._cancelLongPress();
          if (elapsed < 500) {
            var hit = self.hitTest(sx, sy);
            var now = Date.now();
            var tapDist = Math.hypot(sx - self._lastTapPos.x, sy - self._lastTapPos.y);
            if (now - self._lastTapTime < self._doubleTapInterval && tapDist < self._tapDistanceThreshold * 2) {
              var dtCbs = self._gestureCallbacks.doubleTap;
              for (var i = 0; i < dtCbs.length; i++) try { dtCbs[i]({ entity: hit ? hit.entity : null, point: hit ? hit.point : null, screenX: sx, screenY: sy }); } catch (err) {}
              self._lastTapTime = 0;
            } else {
              self._lastTapTime = now; self._lastTapPos = { x: sx, y: sy };
              var tapCbs = self._gestureCallbacks.tap;
              for (var j = 0; j < tapCbs.length; j++) try { tapCbs[j]({ entity: hit ? hit.entity : null, point: hit ? hit.point : null, screenX: sx, screenY: sy }); } catch (err) {}
              if (hit && self._selectableEntities.has(hit.entity)) {
                var wasSelected = self._selectedEntities.has(hit.entity);
                if (wasSelected) self._selectedEntities.delete(hit.entity); else self._selectedEntities.add(hit.entity);
                self._applySelectionHighlight(hit.entity, !wasSelected);
                self._fireSelectCallbacks(hit.entity, !wasSelected);
              }
            }
          }
        }
        if (self._pointers.size === 0) {
          self._isDragging = false; self._gestureConsumed = false;
          if (self._wantsAutoRotate) self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
        }
      });

      canvas.addEventListener('pointercancel', function(e) {
        self._pointers.delete(e.pointerId);
        if (self._activeDragEntity !== null) self._endEntityDrag(0, 0);
        self._cancelLongPress();
        if (self._pointers.size === 0) { self._isDragging = false; self._gestureConsumed = false; if (self._wantsAutoRotate) self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000); }
      });

      canvas.addEventListener('pointerleave', function(e) {
        self._pointers.delete(e.pointerId); self._cancelLongPress();
        if (self._pointers.size === 0) {
          self._isDragging = false;
          if (self._activeDragEntity !== null) self._endEntityDrag(0, 0);
          if (self._wantsAutoRotate) self._autoRotateTimer = setTimeout(function() { self._autoRotate = true; }, 3000);
        }
      });

      canvas.addEventListener('wheel', function(e) {
        e.preventDefault(); self._orbitRadius *= (1 + e.deltaY * 0.001); self._orbitRadius = Math.max(0.5, Math.min(50, self._orbitRadius));
      }, { passive: false });
    }

    _setupResizeObserver() {
      var self = this;
      this._resizeObserver = new ResizeObserver(function() {
        var canvas = self._canvas;
        var dpr = Math.min(devicePixelRatio, 2);
        canvas.width = canvas.clientWidth * dpr; canvas.height = canvas.clientHeight * dpr;
        self._view.setViewport([0, 0, canvas.width, canvas.height]);
        self._camera.setProjectionFov(self._fov || 45, canvas.width / canvas.height, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
      });
      this._resizeObserver.observe(this._canvas);
    }

    // =====================================================================
    // Unified render loop — integrates ALL subsystem updates
    // =====================================================================

    _startRenderLoop() {
      var self = this;
      function render() {
        if (!self._running) return;

        // Delta time
        var now = performance.now() / 1000;
        var deltaTime = now - self._lastFrameTime;
        self._lastFrameTime = now;
        deltaTime = Math.min(deltaTime, 0.1); // clamp to 100ms max

        // Auto-rotate
        if (self._autoRotate) self._angle += 0.00873;

        // Inertia
        if (!self._isDragging) {
          self._angle += self._velocityAngle; self._orbitHeight += self._velocityHeight;
          self._velocityAngle *= self._dampingFactor; self._velocityHeight *= self._dampingFactor;
          if (Math.abs(self._velocityAngle) < 0.00005) self._velocityAngle = 0;
          if (Math.abs(self._velocityHeight) < 0.00005) self._velocityHeight = 0;
        }

        // Update scene graph transforms
        self._updateSceneGraph();

        // Update animations
        self._updateAnimations(deltaTime);

        // Update video textures
        for (var v = 0; v < self._mediaNodes.length; v++) {
          if (self._mediaNodes[v]._updateTexture) self._mediaNodes[v]._updateTexture();
        }

        // Update billboards (face camera)
        self._updateBillboards();

        // Camera
        var t = self._orbitTarget, r = self._orbitRadius, h = self._orbitHeight;
        self._camera.lookAt(
          [t[0] + Math.sin(self._angle) * r, h, t[2] + Math.cos(self._angle) * r], t, [0, 1, 0]
        );

        // Render
        self._engine.execute();
        try {
          if (self._renderer.beginFrame(self._swapChain)) {
            self._renderer.renderView(self._view);
            self._renderer.endFrame();
          }
        } catch (e) { console.error('SceneView render error:', e.message); self._running = false; }
        requestAnimationFrame(render);
      }
      render();
    }

    _updateSceneGraph() {
      this._root.traverse(function(node) {
        if (node._entity && node._worldMatrixDirty) node._applyTransformToFilament();
      });
    }

    _updateBillboards() {
      if (this._billboards.length === 0) return;
      var cam = this._getCameraMatrices();
      var camPos = cam.eye;
      for (var i = 0; i < this._billboards.length; i++) {
        var bb = this._billboards[i];
        try {
          var tm = this._engine.getTransformManager();
          var inst = tm.getInstance(bb.entity);
          if (!inst) continue;
          var transform = tm.getTransform(inst);
          var pos = [transform[12], transform[13], transform[14]];
          var dir = Vec3.normalize(Vec3.sub(camPos, pos));
          var angle = Math.atan2(dir[0], dir[2]);
          var scaleX = Math.sqrt(transform[0]*transform[0] + transform[1]*transform[1] + transform[2]*transform[2]);
          var scaleY = Math.sqrt(transform[4]*transform[4] + transform[5]*transform[5] + transform[6]*transform[6]);
          var newTransform = Mat4.fromTRS(pos, [0, angle * 180 / Math.PI, 0], [scaleX, scaleY, 1]);
          tm.setTransform(inst, newTransform);
        } catch (e) {}
      }
    }
  }

  // =========================================================================
  // Engine creation
  // =========================================================================

  var _activeCanvases = new Set();

  function _createEngine(canvasOrId, options) {
    options = options || {};
    var canvas = typeof canvasOrId === 'string' ? document.getElementById(canvasOrId) : canvasOrId;
    if (!canvas) throw new Error('Canvas not found: ' + canvasOrId);
    if (_activeCanvases.has(canvas)) { console.warn('SceneView: Canvas already initialized'); return null; }
    _activeCanvases.add(canvas);

    var dpr = Math.min(devicePixelRatio, 2);
    var cssW = canvas.clientWidth || canvas.offsetWidth || 500;
    var cssH = canvas.clientHeight || canvas.offsetHeight || 500;
    canvas.width = cssW * dpr; canvas.height = cssH * dpr;

    var engine = Filament.Engine.create(canvas);
    var scene = engine.createScene();
    var renderer = engine.createRenderer();
    var cameraEntity = Filament.EntityManager.get().create();
    var camera = engine.createCamera(cameraEntity);
    var view = engine.createView();
    var swapChain = engine.createSwapChain();

    view.setCamera(camera); view.setScene(scene);
    view.setViewport([0, 0, canvas.width, canvas.height]);

    var bg = options.backgroundColor || [0.05, 0.06, 0.1, 1.0];
    renderer.setClearOptions({ clearColor: bg, clear: true });

    var fov = options.fov || 45;
    camera.setProjectionFov(fov, canvas.width / canvas.height, 0.1, 1000, Filament.Camera$Fov.VERTICAL);
    camera.lookAt([0, 1, 5], [0, 0, 0], [0, 1, 0]);

    try { view.setAmbientOcclusionOptions({ enabled: true, radius: 0.3, bias: 0.0005, intensity: 1.0, quality: 1 }); } catch (e) {}

    // 3-point studio lighting
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

    // IBL
    var iblUrl = options.iblUrl || 'environments/neutral_ibl.ktx';
    fetch(iblUrl)
      .then(function(r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.arrayBuffer().then(function(ab) { return new Uint8Array(ab); }); })
      .then(function(buffer) {
        try {
          var ibl = engine.createIblFromKtx1(buffer);
          ibl.setIntensity(options.iblIntensity || 40000);
          scene.setIndirectLight(ibl);
        } catch (e) { _applySyntheticIBL(engine, scene); }
      })
      .catch(function() { _applySyntheticIBL(engine, scene); });

    var loader = engine.createAssetLoader();
    var instance = new SceneViewInstance(canvas, engine, scene, renderer, view, swapChain, camera, cameraEntity, loader);
    instance._fov = fov;
    instance._sunEntity = sun;
    instance._fillEntity = fill;
    instance._backEntity = back;
    if (options.autoRotate === false) instance.setAutoRotate(false);
    return instance;
  }

  function _applySyntheticIBL(engine, scene) {
    try {
      var ibl = Filament.IndirectLight.Builder()
        .irradiance(3, [0.65,0.65,0.70, 0.10,0.10,0.12, 0.15,0.15,0.18, -0.02,-0.02,-0.01, 0.04,0.04,0.05, 0.08,0.08,0.10, 0.01,0.01,0.01, -0.02,-0.02,-0.02, 0.03,0.03,0.03])
        .intensity(35000).build(engine);
      scene.setIndirectLight(ibl);
    } catch (e) {}
  }

  function create(canvasOrId, options) {
    return _ensureFilament().then(function() {
      return new Promise(function(resolve, reject) {
        if (typeof Filament.Engine !== 'undefined') {
          try { var inst = _createEngine(canvasOrId, options); if (inst) resolve(inst); else reject(new Error('SceneView: Canvas already initialized')); } catch (e) { reject(e); }
          return;
        }
        Filament.init([], function() {
          try { var inst = _createEngine(canvasOrId, options); if (inst) resolve(inst); else reject(new Error('SceneView: Canvas already initialized')); } catch (e) { reject(e); }
        });
      });
    });
  }

  function modelViewer(canvasOrId, modelUrl, options) {
    return create(canvasOrId, options).then(function(instance) { return instance.loadModel(modelUrl); });
  }

  // =========================================================================
  // Public API
  // =========================================================================

  global.SceneView = {
    version: '2.0.0',
    create: create,
    modelViewer: modelViewer,
    setMaterialsPath: setMaterialsPath,

    // Math utilities (exposed for advanced usage)
    Vec3: Vec3,
    Mat4: Mat4,
    Collision: Collision,

    // Node classes (for instanceof checks, extending, or advanced usage)
    SceneNode: SceneNode,
    ModelNode: ModelNode,
    LightNode: LightNode,
    GeometryNode: GeometryNode,
    GroupNode: GroupNode
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
