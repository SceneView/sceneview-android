/**
 * SceneView Web — Simple 3D for the web.
 *
 * One line to render a 3D model:
 *   SceneView.modelViewer("canvas", "model.glb")
 *
 * Procedural geometry:
 *   var sv = await SceneView.create("canvas");
 *   var cube = await sv.createCube({ size: [1,1,1], color: [1,0,0,1] });
 *   var sphere = await sv.createSphere({ radius: 0.5, color: '#00ff00' });
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
  // This avoids dynamic script injection issues with WASM resolution.

  // =========================================================================
  // Internal: material cache and base path resolution
  // =========================================================================

  // Cached compiled Filament Material objects (keyed by type: 'lit', 'unlit', 'transparent')
  var _materialCache = {};

  // Base path for .filamat material files — relative to HTML page
  var _materialsBasePath = 'materials/';

  /**
   * Resolve the materials base path. Looks for the sceneview.js script tag
   * to compute a sibling path, falling back to 'materials/'.
   */
  function _resolveMaterialsPath() {
    try {
      var scripts = document.querySelectorAll('script[src]');
      for (var i = 0; i < scripts.length; i++) {
        var src = scripts[i].getAttribute('src');
        if (src && src.indexOf('sceneview') !== -1 && src.indexOf('.js') !== -1) {
          // e.g. "js/sceneview.js" -> "js/../materials/" -> "materials/"
          var dir = src.substring(0, src.lastIndexOf('/') + 1);
          _materialsBasePath = dir + '../materials/';
          return;
        }
      }
    } catch (e) { /* use default */ }
  }

  // Run once at load time
  _resolveMaterialsPath();

  /**
   * Load and cache a compiled .filamat material.
   * @param {Filament.Engine} engine
   * @param {string} type - 'lit', 'unlit', or 'transparent'
   * @returns {Promise<Filament.Material>}
   */
  function _loadMaterial(engine, type) {
    if (_materialCache[type]) return Promise.resolve(_materialCache[type]);

    var filename = {
      'lit': 'lit_colored.filamat',
      'unlit': 'unlit_colored.filamat',
      'transparent': 'transparent_colored.filamat'
    }[type];

    if (!filename) return Promise.reject(new Error('Unknown material type: ' + type));

    var url = _materialsBasePath + filename;
    return fetch(url)
      .then(function(resp) {
        if (!resp.ok) throw new Error('Failed to load material: ' + url + ' (' + resp.status + ')');
        return resp.arrayBuffer();
      })
      .then(function(buffer) {
        var mat = engine.createMaterial(new Uint8Array(buffer));
        _materialCache[type] = mat;
        console.log('SceneView: Material loaded — ' + type + ' (' + Math.round(buffer.byteLength / 1024) + 'KB)');
        return mat;
      });
  }

  // =========================================================================
  // Internal: color parsing utility
  // =========================================================================

  /**
   * Parse a color into [r, g, b, a] floats (0-1 range).
   * Accepts:
   *   - [r, g, b] or [r, g, b, a] arrays (0-1 range)
   *   - hex string '#rrggbb' or '#rrggbbaa'
   *   - CSS-style 'rgb(r,g,b)' (0-255 range)
   *   - undefined -> default white [1, 1, 1, 1]
   */
  function _parseColor(color) {
    if (!color) return [1, 1, 1, 1];

    if (Array.isArray(color)) {
      return [
        color[0] || 0,
        color[1] || 0,
        color[2] || 0,
        color[3] !== undefined ? color[3] : 1
      ];
    }

    if (typeof color === 'string') {
      // Hex: #rgb, #rrggbb, #rrggbbaa
      if (color[0] === '#') {
        var hex = color.substring(1);
        if (hex.length === 3) {
          hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
        }
        var r = parseInt(hex.substring(0, 2), 16) / 255;
        var g = parseInt(hex.substring(2, 4), 16) / 255;
        var b = parseInt(hex.substring(4, 6), 16) / 255;
        var a = hex.length >= 8 ? parseInt(hex.substring(6, 8), 16) / 255 : 1;
        return [r, g, b, a];
      }

      // rgb(r, g, b)
      var match = color.match(/rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)/);
      if (match) {
        return [parseInt(match[1]) / 255, parseInt(match[2]) / 255, parseInt(match[3]) / 255, 1];
      }
    }

    return [1, 1, 1, 1];
  }

  // =========================================================================
  // Internal: geometry generation
  // =========================================================================

  /**
   * Build a Filament entity from raw geometry data + material options.
   *
   * @param {SceneViewInstance} sv - The SceneView instance
   * @param {Object} geom - { positions: Float32Array, normals: Float32Array, uvs: Float32Array, indices: Uint16Array }
   * @param {Object} matOpts - { color, metallic, roughness, reflectance, unlit, transparent }
   * @param {Object} bbox - { min: [x,y,z], max: [x,y,z] }
   * @param {number} primitiveType - Filament primitive type (TRIANGLES, LINES, etc.)
   * @returns {Promise<number>} - Filament Entity handle
   */
  function _buildEntity(sv, geom, matOpts, bbox, primitiveType) {
    var engine = sv._engine;
    var scene = sv._scene;

    // Determine material type
    var matType = matOpts.unlit ? 'unlit' : (matOpts.transparent ? 'transparent' : 'lit');

    return _loadMaterial(engine, matType).then(function(material) {
      var matInstance = material.createInstance();
      var color = _parseColor(matOpts.color);

      // Set material parameters
      matInstance.setColor4Parameter('baseColor', Filament.RgbaType.sRGB, color);
      if (!matOpts.unlit) {
        matInstance.setFloatParameter('metallic', matOpts.metallic !== undefined ? matOpts.metallic : 0.0);
        matInstance.setFloatParameter('roughness', matOpts.roughness !== undefined ? matOpts.roughness : 0.4);
        matInstance.setFloatParameter('reflectance', matOpts.reflectance !== undefined ? matOpts.reflectance : 0.5);
      }

      // Compute tangent quaternions from normals using SurfaceOrientation
      var nVerts = geom.positions.length / 3;
      var tangents;
      try {
        var sob = new Filament.SurfaceOrientation$Builder();
        sob.vertexCount(nVerts);
        sob.normals(geom.normals, 0);
        if (geom.positions) sob.positions(geom.positions, 0);
        var orientation = sob.build();
        tangents = orientation.getQuats(nVerts);
        orientation.delete();
      } catch (e) {
        // Fallback: create identity tangent quaternions (SHORT4)
        tangents = new Int16Array(nVerts * 4);
        for (var i = 0; i < nVerts; i++) {
          tangents[i * 4 + 3] = 32767; // w = 1.0 as snorm16
        }
      }

      // Build vertex buffer: 3 attributes — position (FLOAT3), tangents (SHORT4), uv (FLOAT2)
      var vb = Filament.VertexBuffer.Builder()
        .vertexCount(nVerts)
        .bufferCount(3)
        .attribute(Filament.VertexAttribute.POSITION, 0, Filament.VertexBuffer$AttributeType.FLOAT3, 0, 12)
        .attribute(Filament.VertexAttribute.TANGENTS, 1, Filament.VertexBuffer$AttributeType.SHORT4, 0, 8)
        .normalized(Filament.VertexAttribute.TANGENTS)
        .attribute(Filament.VertexAttribute.UV0, 2, Filament.VertexBuffer$AttributeType.FLOAT2, 0, 8)
        .build(engine);

      vb.setBufferAt(engine, 0, geom.positions);
      vb.setBufferAt(engine, 1, tangents);
      vb.setBufferAt(engine, 2, geom.uvs);

      // Build index buffer
      var ib = Filament.IndexBuffer.Builder()
        .indexCount(geom.indices.length)
        .bufferType(Filament.IndexBuffer$IndexType.USHORT)
        .build(engine);
      ib.setBuffer(engine, geom.indices);

      // Create entity
      var entity = Filament.EntityManager.get().create();
      var primType = primitiveType || Filament.RenderableManager$PrimitiveType.TRIANGLES;

      Filament.RenderableManager.Builder(1)
        .boundingBox(bbox)
        .material(0, matInstance)
        .geometry(0, primType, vb, ib)
        .culling(false)
        .receiveShadows(true)
        .castShadows(true)
        .build(engine, entity);

      // Set initial position from center option
      var center = matOpts.center || [0, 0, 0];
      var tcm = engine.getTransformManager();
      var inst = tcm.getInstance(entity);
      tcm.setTransform(inst, _translationMatrix(center[0], center[1], center[2]));

      // Add to scene
      scene.addEntity(entity);

      // Track for cleanup
      if (!sv._geometryEntities) sv._geometryEntities = [];
      sv._geometryEntities.push({
        entity: entity,
        vertexBuffer: vb,
        indexBuffer: ib,
        matInstance: matInstance
      });

      return entity;
    });
  }

  // =========================================================================
  // Internal: 4x4 matrix helpers (column-major for Filament)
  // =========================================================================

  function _identityMatrix() {
    return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1];
  }

  function _translationMatrix(x, y, z) {
    return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1];
  }

  function _scaleMatrix(sx, sy, sz) {
    return [sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, sz, 0, 0, 0, 0, 1];
  }

  /** Euler rotation matrix (XYZ order, degrees) */
  function _rotationMatrix(rx, ry, rz) {
    var toRad = Math.PI / 180;
    var ax = rx * toRad, ay = ry * toRad, az = rz * toRad;
    var cx = Math.cos(ax), sx = Math.sin(ax);
    var cy = Math.cos(ay), sy = Math.sin(ay);
    var cz = Math.cos(az), sz = Math.sin(az);
    // Combined XYZ rotation matrix (column-major)
    return [
      cy * cz,                    cy * sz,                    -sy,    0,
      sx * sy * cz - cx * sz,     sx * sy * sz + cx * cz,     sx * cy, 0,
      cx * sy * cz + sx * sz,     cx * sy * sz - sx * cz,     cx * cy, 0,
      0, 0, 0, 1
    ];
  }

  /** Multiply two 4x4 column-major matrices: result = a * b */
  function _mat4Mul(a, b) {
    var r = new Array(16);
    for (var col = 0; col < 4; col++) {
      for (var row = 0; row < 4; row++) {
        r[col * 4 + row] =
          a[0 * 4 + row] * b[col * 4 + 0] +
          a[1 * 4 + row] * b[col * 4 + 1] +
          a[2 * 4 + row] * b[col * 4 + 2] +
          a[3 * 4 + row] * b[col * 4 + 3];
      }
    }
    return r;
  }

  // =========================================================================
  // Geometry generators — cube, sphere, cylinder, cone, plane
  // =========================================================================

  /**
   * Generate a box with per-face normals and UVs.
   * @param {number} w - width (X)
   * @param {number} h - height (Y)
   * @param {number} d - depth (Z)
   * @returns {{ positions: Float32Array, normals: Float32Array, uvs: Float32Array, indices: Uint16Array }}
   */
  function _genCube(w, h, d) {
    var hw = w / 2, hh = h / 2, hd = d / 2;

    // 6 faces * 4 vertices = 24 vertices
    // Face order: +X, -X, +Y, -Y, +Z, -Z
    var positions = new Float32Array([
      // +X
       hw, -hh,  hd,   hw,  hh,  hd,   hw,  hh, -hd,   hw, -hh, -hd,
      // -X
      -hw, -hh, -hd,  -hw,  hh, -hd,  -hw,  hh,  hd,  -hw, -hh,  hd,
      // +Y
      -hw,  hh,  hd,  -hw,  hh, -hd,   hw,  hh, -hd,   hw,  hh,  hd,
      // -Y
      -hw, -hh, -hd,  -hw, -hh,  hd,   hw, -hh,  hd,   hw, -hh, -hd,
      // +Z
      -hw, -hh,  hd,  -hw,  hh,  hd,   hw,  hh,  hd,   hw, -hh,  hd,
      // -Z
       hw, -hh, -hd,   hw,  hh, -hd,  -hw,  hh, -hd,  -hw, -hh, -hd
    ]);

    var normals = new Float32Array([
      // +X
      1, 0, 0,  1, 0, 0,  1, 0, 0,  1, 0, 0,
      // -X
      -1, 0, 0,  -1, 0, 0,  -1, 0, 0,  -1, 0, 0,
      // +Y
      0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0,
      // -Y
      0, -1, 0,  0, -1, 0,  0, -1, 0,  0, -1, 0,
      // +Z
      0, 0, 1,  0, 0, 1,  0, 0, 1,  0, 0, 1,
      // -Z
      0, 0, -1,  0, 0, -1,  0, 0, -1,  0, 0, -1
    ]);

    var uvs = new Float32Array([
      // Each face gets full 0-1 UVs
      0, 0,  0, 1,  1, 1,  1, 0,
      0, 0,  0, 1,  1, 1,  1, 0,
      0, 0,  0, 1,  1, 1,  1, 0,
      0, 0,  0, 1,  1, 1,  1, 0,
      0, 0,  0, 1,  1, 1,  1, 0,
      0, 0,  0, 1,  1, 1,  1, 0
    ]);

    // 6 faces * 2 triangles * 3 indices = 36 indices
    var indices = new Uint16Array([
       0,  1,  2,   0,  2,  3,   // +X
       4,  5,  6,   4,  6,  7,   // -X
       8,  9, 10,   8, 10, 11,   // +Y
      12, 13, 14,  12, 14, 15,   // -Y
      16, 17, 18,  16, 18, 19,   // +Z
      20, 21, 22,  20, 22, 23    // -Z
    ]);

    return { positions: positions, normals: normals, uvs: uvs, indices: indices };
  }

  /**
   * Generate a UV sphere.
   * @param {number} radius
   * @param {number} stacks - vertical divisions
   * @param {number} slices - horizontal divisions
   */
  function _genSphere(radius, stacks, slices) {
    stacks = stacks || 16;
    slices = slices || 32;
    var nVerts = (stacks + 1) * (slices + 1);
    var nIndices = stacks * slices * 6;

    var positions = new Float32Array(nVerts * 3);
    var normals = new Float32Array(nVerts * 3);
    var uvs = new Float32Array(nVerts * 2);
    var indices = new Uint16Array(nIndices);

    var vi = 0, ui = 0;
    for (var stack = 0; stack <= stacks; stack++) {
      var phi = (stack / stacks) * Math.PI;
      var sinPhi = Math.sin(phi), cosPhi = Math.cos(phi);

      for (var slice = 0; slice <= slices; slice++) {
        var theta = (slice / slices) * 2 * Math.PI;
        var sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

        var nx = sinPhi * cosTheta;
        var ny = cosPhi;
        var nz = sinPhi * sinTheta;

        positions[vi] = nx * radius;
        positions[vi + 1] = ny * radius;
        positions[vi + 2] = nz * radius;
        normals[vi] = nx;
        normals[vi + 1] = ny;
        normals[vi + 2] = nz;
        vi += 3;

        uvs[ui] = slice / slices;
        uvs[ui + 1] = stack / stacks;
        ui += 2;
      }
    }

    var ii = 0;
    for (var stack = 0; stack < stacks; stack++) {
      for (var slice = 0; slice < slices; slice++) {
        var a = stack * (slices + 1) + slice;
        var b = a + slices + 1;

        indices[ii++] = a;
        indices[ii++] = b;
        indices[ii++] = a + 1;

        indices[ii++] = a + 1;
        indices[ii++] = b;
        indices[ii++] = b + 1;
      }
    }

    return { positions: positions, normals: normals, uvs: uvs, indices: indices };
  }

  /**
   * Generate a cylinder with top and bottom caps.
   * @param {number} radius
   * @param {number} height
   * @param {number} sides - number of radial segments
   */
  function _genCylinder(radius, height, sides) {
    sides = sides || 32;
    var hh = height / 2;

    // Side vertices: (sides+1) * 2 (top ring + bottom ring with wrap)
    // Top cap: 1 center + sides+1
    // Bottom cap: 1 center + sides+1
    var nSideVerts = (sides + 1) * 2;
    var nCapVerts = (sides + 1 + 1) * 2; // top + bottom caps
    var nVerts = nSideVerts + nCapVerts;
    var nSideIdx = sides * 6;
    var nCapIdx = sides * 3 * 2; // top + bottom
    var nIndices = nSideIdx + nCapIdx;

    var positions = new Float32Array(nVerts * 3);
    var normals = new Float32Array(nVerts * 3);
    var uvs = new Float32Array(nVerts * 2);
    var indices = new Uint16Array(nIndices);

    var vi = 0, ui = 0, ii = 0;

    // === Side vertices ===
    for (var i = 0; i <= sides; i++) {
      var angle = (i / sides) * Math.PI * 2;
      var nx = Math.cos(angle), nz = Math.sin(angle);
      var u = i / sides;

      // Top vertex
      positions[vi] = nx * radius; positions[vi + 1] = hh; positions[vi + 2] = nz * radius;
      normals[vi] = nx; normals[vi + 1] = 0; normals[vi + 2] = nz;
      vi += 3;
      uvs[ui] = u; uvs[ui + 1] = 1; ui += 2;

      // Bottom vertex
      positions[vi] = nx * radius; positions[vi + 1] = -hh; positions[vi + 2] = nz * radius;
      normals[vi] = nx; normals[vi + 1] = 0; normals[vi + 2] = nz;
      vi += 3;
      uvs[ui] = u; uvs[ui + 1] = 0; ui += 2;
    }

    // Side indices
    for (var i = 0; i < sides; i++) {
      var top = i * 2, bot = top + 1;
      var nextTop = top + 2, nextBot = top + 3;
      indices[ii++] = top;
      indices[ii++] = bot;
      indices[ii++] = nextTop;
      indices[ii++] = nextTop;
      indices[ii++] = bot;
      indices[ii++] = nextBot;
    }

    // === Top cap ===
    var topCenterIdx = vi / 3;
    positions[vi] = 0; positions[vi + 1] = hh; positions[vi + 2] = 0;
    normals[vi] = 0; normals[vi + 1] = 1; normals[vi + 2] = 0;
    vi += 3;
    uvs[ui] = 0.5; uvs[ui + 1] = 0.5; ui += 2;

    for (var i = 0; i <= sides; i++) {
      var angle = (i / sides) * Math.PI * 2;
      var nx = Math.cos(angle), nz = Math.sin(angle);
      positions[vi] = nx * radius; positions[vi + 1] = hh; positions[vi + 2] = nz * radius;
      normals[vi] = 0; normals[vi + 1] = 1; normals[vi + 2] = 0;
      vi += 3;
      uvs[ui] = nx * 0.5 + 0.5; uvs[ui + 1] = nz * 0.5 + 0.5; ui += 2;
    }

    for (var i = 0; i < sides; i++) {
      indices[ii++] = topCenterIdx;
      indices[ii++] = topCenterIdx + 1 + i;
      indices[ii++] = topCenterIdx + 2 + i;
    }

    // === Bottom cap ===
    var botCenterIdx = vi / 3;
    positions[vi] = 0; positions[vi + 1] = -hh; positions[vi + 2] = 0;
    normals[vi] = 0; normals[vi + 1] = -1; normals[vi + 2] = 0;
    vi += 3;
    uvs[ui] = 0.5; uvs[ui + 1] = 0.5; ui += 2;

    for (var i = 0; i <= sides; i++) {
      var angle = (i / sides) * Math.PI * 2;
      var nx = Math.cos(angle), nz = Math.sin(angle);
      positions[vi] = nx * radius; positions[vi + 1] = -hh; positions[vi + 2] = nz * radius;
      normals[vi] = 0; normals[vi + 1] = -1; normals[vi + 2] = 0;
      vi += 3;
      uvs[ui] = nx * 0.5 + 0.5; uvs[ui + 1] = nz * 0.5 + 0.5; ui += 2;
    }

    for (var i = 0; i < sides; i++) {
      indices[ii++] = botCenterIdx;
      indices[ii++] = botCenterIdx + 2 + i;
      indices[ii++] = botCenterIdx + 1 + i;
    }

    return { positions: positions, normals: normals, uvs: uvs, indices: indices };
  }

  /**
   * Generate a cone with a base cap.
   * @param {number} radius
   * @param {number} height
   * @param {number} sides
   */
  function _genCone(radius, height, sides) {
    sides = sides || 32;
    var hh = height / 2;

    // Side vertices: (sides+1) * 2 (apex ring duplicated + base ring)
    // Base cap: 1 center + sides+1
    var nSideVerts = (sides + 1) * 2;
    var nCapVerts = sides + 2;
    var nVerts = nSideVerts + nCapVerts;
    var nSideIdx = sides * 3;
    var nCapIdx = sides * 3;
    var nIndices = nSideIdx + nCapIdx;

    var positions = new Float32Array(nVerts * 3);
    var normals = new Float32Array(nVerts * 3);
    var uvs = new Float32Array(nVerts * 2);
    var indices = new Uint16Array(nIndices);

    // Compute the cone's slope angle for normals
    var slopeLen = Math.sqrt(radius * radius + height * height);
    var nY = radius / slopeLen;
    var nR = height / slopeLen;

    var vi = 0, ui = 0, ii = 0;

    // === Side vertices ===
    for (var i = 0; i <= sides; i++) {
      var angle = (i / sides) * Math.PI * 2;
      var cosA = Math.cos(angle), sinA = Math.sin(angle);
      var u = i / sides;

      // Normal for this side face
      var snx = cosA * nR, sny = nY, snz = sinA * nR;

      // Apex vertex (top)
      positions[vi] = 0; positions[vi + 1] = hh; positions[vi + 2] = 0;
      normals[vi] = snx; normals[vi + 1] = sny; normals[vi + 2] = snz;
      vi += 3;
      uvs[ui] = u; uvs[ui + 1] = 1; ui += 2;

      // Base vertex (bottom)
      positions[vi] = cosA * radius; positions[vi + 1] = -hh; positions[vi + 2] = sinA * radius;
      normals[vi] = snx; normals[vi + 1] = sny; normals[vi + 2] = snz;
      vi += 3;
      uvs[ui] = u; uvs[ui + 1] = 0; ui += 2;
    }

    // Side indices (triangles from apex to base)
    for (var i = 0; i < sides; i++) {
      var apex = i * 2;
      var base = apex + 1;
      var nextBase = base + 2;
      indices[ii++] = apex;
      indices[ii++] = base;
      indices[ii++] = nextBase;
    }

    // === Base cap ===
    var baseCenterIdx = vi / 3;
    positions[vi] = 0; positions[vi + 1] = -hh; positions[vi + 2] = 0;
    normals[vi] = 0; normals[vi + 1] = -1; normals[vi + 2] = 0;
    vi += 3;
    uvs[ui] = 0.5; uvs[ui + 1] = 0.5; ui += 2;

    for (var i = 0; i <= sides; i++) {
      var angle = (i / sides) * Math.PI * 2;
      var cosA = Math.cos(angle), sinA = Math.sin(angle);
      positions[vi] = cosA * radius; positions[vi + 1] = -hh; positions[vi + 2] = sinA * radius;
      normals[vi] = 0; normals[vi + 1] = -1; normals[vi + 2] = 0;
      vi += 3;
      uvs[ui] = cosA * 0.5 + 0.5; uvs[ui + 1] = sinA * 0.5 + 0.5; ui += 2;
    }

    for (var i = 0; i < sides; i++) {
      indices[ii++] = baseCenterIdx;
      indices[ii++] = baseCenterIdx + 2 + i;
      indices[ii++] = baseCenterIdx + 1 + i;
    }

    return { positions: positions, normals: normals, uvs: uvs, indices: indices };
  }

  /**
   * Generate a plane (quad) with a given normal direction.
   * @param {number} w - width
   * @param {number} h - height
   * @param {Array} normal - [nx, ny, nz] default [0, 1, 0]
   */
  function _genPlane(w, h, normal) {
    normal = normal || [0, 1, 0];
    var hw = w / 2, hh = h / 2;

    // Default plane lies in XZ plane (Y-up normal)
    // We'll generate it in XZ then rotate if needed
    var nx = normal[0], ny = normal[1], nz = normal[2];
    var len = Math.sqrt(nx * nx + ny * ny + nz * nz);
    if (len > 0) { nx /= len; ny /= len; nz /= len; }

    // Generate basis vectors for the plane
    var up = [0, 1, 0];
    // If normal is nearly parallel to up, use a different reference
    if (Math.abs(ny) > 0.99) {
      up = [0, 0, 1];
    }
    // tangent = normalize(up x normal)
    var tx = up[1] * nz - up[2] * ny;
    var ty = up[2] * nx - up[0] * nz;
    var tz = up[0] * ny - up[1] * nx;
    var tLen = Math.sqrt(tx * tx + ty * ty + tz * tz);
    if (tLen > 0) { tx /= tLen; ty /= tLen; tz /= tLen; }

    // bitangent = normal x tangent
    var bx = ny * tz - nz * ty;
    var by = nz * tx - nx * tz;
    var bz = nx * ty - ny * tx;

    // 4 corners: (-hw,-hh), (-hw,+hh), (+hw,+hh), (+hw,-hh) in tangent/bitangent space
    var positions = new Float32Array([
      -hw * tx + -hh * bx, -hw * ty + -hh * by, -hw * tz + -hh * bz,
      -hw * tx +  hh * bx, -hw * ty +  hh * by, -hw * tz +  hh * bz,
       hw * tx +  hh * bx,  hw * ty +  hh * by,  hw * tz +  hh * bz,
       hw * tx + -hh * bx,  hw * ty + -hh * by,  hw * tz + -hh * bz
    ]);

    var normals = new Float32Array([
      nx, ny, nz,  nx, ny, nz,  nx, ny, nz,  nx, ny, nz
    ]);

    var uvs = new Float32Array([
      0, 0,  0, 1,  1, 1,  1, 0
    ]);

    var indices = new Uint16Array([
      0, 1, 2,  0, 2, 3
    ]);

    return { positions: positions, normals: normals, uvs: uvs, indices: indices };
  }

  /**
   * Generate a thin cylinder between two points (for line rendering).
   * @param {Array} from - [x, y, z]
   * @param {Array} to - [x, y, z]
   * @param {number} thickness - diameter
   * @param {number} sides - radial segments
   */
  function _genLineCylinder(from, to, thickness, sides) {
    sides = sides || 8;
    var radius = thickness / 2;

    // Direction vector
    var dx = to[0] - from[0], dy = to[1] - from[1], dz = to[2] - from[2];
    var length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (length < 0.0001) length = 0.0001;

    // Normalized direction
    var dirX = dx / length, dirY = dy / length, dirZ = dz / length;

    // Find a perpendicular vector
    var perpX, perpY, perpZ;
    if (Math.abs(dirY) < 0.99) {
      // Cross with up [0,1,0]
      perpX = dirZ; perpY = 0; perpZ = -dirX;
    } else {
      // Cross with right [1,0,0]
      perpX = 0; perpY = -dirZ; perpZ = dirY;
    }
    var pLen = Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
    if (pLen > 0) { perpX /= pLen; perpY /= pLen; perpZ /= pLen; }

    // Second perpendicular: dir x perp
    var perp2X = dirY * perpZ - dirZ * perpY;
    var perp2Y = dirZ * perpX - dirX * perpZ;
    var perp2Z = dirX * perpY - dirY * perpX;

    // Midpoint
    var mx = (from[0] + to[0]) / 2;
    var my = (from[1] + to[1]) / 2;
    var mz = (from[2] + to[2]) / 2;

    var nVerts = (sides + 1) * 2;
    var nIndices = sides * 6;
    var positions = new Float32Array(nVerts * 3);
    var normals = new Float32Array(nVerts * 3);
    var uvs = new Float32Array(nVerts * 2);
    var indices = new Uint16Array(nIndices);

    var vi = 0, ui = 0;
    for (var i = 0; i <= sides; i++) {
      var angle = (i / sides) * Math.PI * 2;
      var cosA = Math.cos(angle), sinA = Math.sin(angle);

      var nx = perpX * cosA + perp2X * sinA;
      var ny = perpY * cosA + perp2Y * sinA;
      var nz = perpZ * cosA + perp2Z * sinA;

      // Start point
      positions[vi] = from[0] + nx * radius;
      positions[vi + 1] = from[1] + ny * radius;
      positions[vi + 2] = from[2] + nz * radius;
      normals[vi] = nx; normals[vi + 1] = ny; normals[vi + 2] = nz;
      vi += 3;
      uvs[ui] = i / sides; uvs[ui + 1] = 0; ui += 2;

      // End point
      positions[vi] = to[0] + nx * radius;
      positions[vi + 1] = to[1] + ny * radius;
      positions[vi + 2] = to[2] + nz * radius;
      normals[vi] = nx; normals[vi + 1] = ny; normals[vi + 2] = nz;
      vi += 3;
      uvs[ui] = i / sides; uvs[ui + 1] = 1; ui += 2;
    }

    var ii = 0;
    for (var i = 0; i < sides; i++) {
      var a = i * 2, b = a + 1, c = a + 2, d = a + 3;
      indices[ii++] = a; indices[ii++] = b; indices[ii++] = c;
      indices[ii++] = c; indices[ii++] = b; indices[ii++] = d;
    }

    return { positions: positions, normals: normals, uvs: uvs, indices: indices };
  }

  /**
   * Generate a path (polyline) as a series of connected thin cylinders.
   * Returns merged geometry for all segments.
   * @param {Array<Array<number>>} points - array of [x,y,z]
   * @param {number} thickness
   * @param {boolean} closed - connect last point to first
   * @param {number} sides - radial segments per segment
   */
  function _genPath(points, thickness, closed, sides) {
    sides = sides || 8;
    if (points.length < 2) throw new Error('Path needs at least 2 points');

    var segments = [];
    for (var i = 0; i < points.length - 1; i++) {
      segments.push(_genLineCylinder(points[i], points[i + 1], thickness, sides));
    }
    if (closed && points.length > 2) {
      segments.push(_genLineCylinder(points[points.length - 1], points[0], thickness, sides));
    }

    // Merge all segment geometries
    var totalVerts = 0, totalIndices = 0;
    for (var i = 0; i < segments.length; i++) {
      totalVerts += segments[i].positions.length / 3;
      totalIndices += segments[i].indices.length;
    }

    var positions = new Float32Array(totalVerts * 3);
    var normals = new Float32Array(totalVerts * 3);
    var uvs = new Float32Array(totalVerts * 2);
    var indices = new Uint16Array(totalIndices);

    var vOff = 0, uOff = 0, iOff = 0, vertOff = 0;
    for (var i = 0; i < segments.length; i++) {
      var seg = segments[i];
      positions.set(seg.positions, vOff);
      normals.set(seg.normals, vOff);
      uvs.set(seg.uvs, uOff);
      for (var j = 0; j < seg.indices.length; j++) {
        indices[iOff + j] = seg.indices[j] + vertOff;
      }
      vOff += seg.positions.length;
      uOff += seg.uvs.length;
      iOff += seg.indices.length;
      vertOff += seg.positions.length / 3;
    }

    return { positions: positions, normals: normals, uvs: uvs, indices: indices };
  }

  // =========================================================================
  // Wait for Filament to be available
  // =========================================================================

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
  // SceneViewInstance
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
      this._angle = 0.785; // Start at ~45deg like model-viewer
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
      this._geometryEntities = []; // Track procedural geometry entities
      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

    // -----------------------------------------------------------------------
    // Model loading (existing API)
    // -----------------------------------------------------------------------

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
        asset.getRenderableEntities().forEach(function(e) { this._scene.remove(e); }.bind(this));
        this._scene.remove(asset.getRoot());
      } catch (e) { /* ignore cleanup errors */ }
    }

    // -----------------------------------------------------------------------
    // Procedural geometry — create primitives
    // -----------------------------------------------------------------------

    /**
     * Create a box/cube entity.
     * @param {Object} options
     * @param {Array<number>} [options.size=[1,1,1]] - [width, height, depth]
     * @param {Array<number>} [options.center=[0,0,0]] - position
     * @param {*} [options.color=[1,1,1,1]] - color (array, hex string, or rgb())
     * @param {number} [options.metallic=0] - metallic factor 0-1
     * @param {number} [options.roughness=0.4] - roughness factor 0-1
     * @param {number} [options.reflectance=0.5] - reflectance factor 0-1
     * @param {boolean} [options.unlit=false] - use unlit material
     * @returns {Promise<number>} Filament Entity handle
     */
    createCube(options) {
      options = options || {};
      var size = options.size || [1, 1, 1];
      var w = size[0], h = size[1], d = size[2];
      var geom = _genCube(w, h, d);
      var bbox = { min: [-w / 2, -h / 2, -d / 2], max: [w / 2, h / 2, d / 2] };
      return _buildEntity(this, geom, options, bbox);
    }

    /**
     * Create a UV sphere entity.
     * @param {Object} options
     * @param {number} [options.radius=0.5] - sphere radius
     * @param {number} [options.stacks=16] - vertical divisions
     * @param {number} [options.slices=32] - horizontal divisions
     * @param {Array<number>} [options.center=[0,0,0]] - position
     * @param {*} [options.color=[1,1,1,1]] - color
     * @param {number} [options.metallic=0]
     * @param {number} [options.roughness=0.4]
     * @returns {Promise<number>} Filament Entity handle
     */
    createSphere(options) {
      options = options || {};
      var r = options.radius !== undefined ? options.radius : 0.5;
      var geom = _genSphere(r, options.stacks, options.slices);
      var bbox = { min: [-r, -r, -r], max: [r, r, r] };
      return _buildEntity(this, geom, options, bbox);
    }

    /**
     * Create a cylinder entity with top and bottom caps.
     * @param {Object} options
     * @param {number} [options.radius=0.5] - cylinder radius
     * @param {number} [options.height=1] - cylinder height
     * @param {number} [options.sideCount=32] - number of radial segments
     * @param {Array<number>} [options.center=[0,0,0]] - position
     * @param {*} [options.color=[1,1,1,1]]
     * @param {number} [options.metallic=0]
     * @param {number} [options.roughness=0.4]
     * @returns {Promise<number>} Filament Entity handle
     */
    createCylinder(options) {
      options = options || {};
      var r = options.radius !== undefined ? options.radius : 0.5;
      var h = options.height !== undefined ? options.height : 1;
      var sides = options.sideCount || 32;
      var geom = _genCylinder(r, h, sides);
      var bbox = { min: [-r, -h / 2, -r], max: [r, h / 2, r] };
      return _buildEntity(this, geom, options, bbox);
    }

    /**
     * Create a cone entity with a base cap.
     * @param {Object} options
     * @param {number} [options.radius=0.5] - base radius
     * @param {number} [options.height=1] - cone height
     * @param {number} [options.sideCount=32] - number of radial segments
     * @param {Array<number>} [options.center=[0,0,0]] - position
     * @param {*} [options.color=[1,1,1,1]]
     * @param {number} [options.metallic=0]
     * @param {number} [options.roughness=0.4]
     * @returns {Promise<number>} Filament Entity handle
     */
    createCone(options) {
      options = options || {};
      var r = options.radius !== undefined ? options.radius : 0.5;
      var h = options.height !== undefined ? options.height : 1;
      var sides = options.sideCount || 32;
      var geom = _genCone(r, h, sides);
      var bbox = { min: [-r, -h / 2, -r], max: [r, h / 2, r] };
      return _buildEntity(this, geom, options, bbox);
    }

    /**
     * Create a plane (quad) entity.
     * @param {Object} options
     * @param {Array<number>} [options.size=[1,1]] - [width, height]
     * @param {Array<number>} [options.normal=[0,1,0]] - plane normal direction
     * @param {Array<number>} [options.center=[0,0,0]] - position
     * @param {*} [options.color=[1,1,1,1]]
     * @param {number} [options.metallic=0]
     * @param {number} [options.roughness=0.4]
     * @returns {Promise<number>} Filament Entity handle
     */
    createPlane(options) {
      options = options || {};
      var size = options.size || [1, 1];
      var normal = options.normal || [0, 1, 0];
      var geom = _genPlane(size[0], size[1], normal);
      // Approximate bounding box
      var maxDim = Math.max(size[0], size[1]) / 2;
      var bbox = { min: [-maxDim, -maxDim, -maxDim], max: [maxDim, maxDim, maxDim] };
      return _buildEntity(this, geom, options, bbox);
    }

    /**
     * Create a line segment (rendered as a thin cylinder).
     * @param {Object} options
     * @param {Array<number>} options.from - [x, y, z] start point
     * @param {Array<number>} options.to - [x, y, z] end point
     * @param {*} [options.color=[1,1,1,1]]
     * @param {number} [options.thickness=0.02] - line thickness (diameter)
     * @param {boolean} [options.unlit=true] - lines are unlit by default
     * @returns {Promise<number>} Filament Entity handle
     */
    createLine(options) {
      options = options || {};
      var from = options.from || [0, 0, 0];
      var to = options.to || [1, 0, 0];
      var thickness = options.thickness || 0.02;

      // Default lines to unlit for cleaner look
      if (options.unlit === undefined) options.unlit = true;
      // Lines don't use center — they're positioned by from/to
      options.center = [0, 0, 0];

      var geom = _genLineCylinder(from, to, thickness, 8);

      var minX = Math.min(from[0], to[0]) - thickness;
      var minY = Math.min(from[1], to[1]) - thickness;
      var minZ = Math.min(from[2], to[2]) - thickness;
      var maxX = Math.max(from[0], to[0]) + thickness;
      var maxY = Math.max(from[1], to[1]) + thickness;
      var maxZ = Math.max(from[2], to[2]) + thickness;
      var bbox = { min: [minX, minY, minZ], max: [maxX, maxY, maxZ] };

      return _buildEntity(this, geom, options, bbox);
    }

    /**
     * Create a polyline path through multiple points (rendered as connected thin cylinders).
     * @param {Object} options
     * @param {Array<Array<number>>} options.points - array of [x,y,z] points
     * @param {*} [options.color=[1,1,1,1]]
     * @param {number} [options.thickness=0.02] - line thickness (diameter)
     * @param {boolean} [options.closed=false] - connect last point to first
     * @param {boolean} [options.unlit=true] - paths are unlit by default
     * @returns {Promise<number>} Filament Entity handle
     */
    createPath(options) {
      options = options || {};
      var points = options.points;
      if (!points || points.length < 2) {
        return Promise.reject(new Error('createPath requires at least 2 points'));
      }
      var thickness = options.thickness || 0.02;
      var closed = options.closed || false;

      // Default paths to unlit
      if (options.unlit === undefined) options.unlit = true;
      options.center = [0, 0, 0];

      var geom = _genPath(points, thickness, closed, 8);

      // Compute bounding box from all points
      var minX = Infinity, minY = Infinity, minZ = Infinity;
      var maxX = -Infinity, maxY = -Infinity, maxZ = -Infinity;
      for (var i = 0; i < points.length; i++) {
        var p = points[i];
        if (p[0] < minX) minX = p[0]; if (p[0] > maxX) maxX = p[0];
        if (p[1] < minY) minY = p[1]; if (p[1] > maxY) maxY = p[1];
        if (p[2] < minZ) minZ = p[2]; if (p[2] > maxZ) maxZ = p[2];
      }
      minX -= thickness; minY -= thickness; minZ -= thickness;
      maxX += thickness; maxY += thickness; maxZ += thickness;
      var bbox = { min: [minX, minY, minZ], max: [maxX, maxY, maxZ] };

      return _buildEntity(this, geom, options, bbox);
    }

    // -----------------------------------------------------------------------
    // Entity transform helpers
    // -----------------------------------------------------------------------

    /**
     * Set the world-space position of an entity.
     * @param {number} entity - Filament Entity handle
     * @param {number} x
     * @param {number} y
     * @param {number} z
     */
    setEntityPosition(entity, x, y, z) {
      var tcm = this._engine.getTransformManager();
      var inst = tcm.getInstance(entity);
      if (!inst) return this;

      // Get current transform, replace translation
      var mat = tcm.getTransform(inst);
      if (mat && mat.length === 16) {
        mat[12] = x; mat[13] = y; mat[14] = z;
        tcm.setTransform(inst, mat);
      } else {
        tcm.setTransform(inst, _translationMatrix(x, y, z));
      }
      return this;
    }

    /**
     * Set the rotation of an entity (Euler angles in degrees, XYZ order).
     * @param {number} entity - Filament Entity handle
     * @param {number} rx - rotation around X axis (degrees)
     * @param {number} ry - rotation around Y axis (degrees)
     * @param {number} rz - rotation around Z axis (degrees)
     */
    setEntityRotation(entity, rx, ry, rz) {
      var tcm = this._engine.getTransformManager();
      var inst = tcm.getInstance(entity);
      if (!inst) return this;

      // Get current transform to preserve translation and scale
      var current = tcm.getTransform(inst);
      var tx = 0, ty = 0, tz = 0;
      if (current && current.length === 16) {
        tx = current[12]; ty = current[13]; tz = current[14];
      }

      // Extract current scale from columns
      var sx = 1, sy = 1, sz = 1;
      if (current && current.length === 16) {
        sx = Math.sqrt(current[0] * current[0] + current[1] * current[1] + current[2] * current[2]);
        sy = Math.sqrt(current[4] * current[4] + current[5] * current[5] + current[6] * current[6]);
        sz = Math.sqrt(current[8] * current[8] + current[9] * current[9] + current[10] * current[10]);
      }

      var rot = _rotationMatrix(rx, ry, rz);
      var scaled = _mat4Mul(_scaleMatrix(sx, sy, sz), rot);
      // Actually: T * R * S — but we want rotation applied after scale
      var newMat = _mat4Mul(rot, _scaleMatrix(sx, sy, sz));
      newMat[12] = tx; newMat[13] = ty; newMat[14] = tz;
      tcm.setTransform(inst, newMat);
      return this;
    }

    /**
     * Set the scale of an entity.
     * @param {number} entity - Filament Entity handle
     * @param {number} sx - scale X
     * @param {number} sy - scale Y
     * @param {number} sz - scale Z
     */
    setEntityScale(entity, sx, sy, sz) {
      var tcm = this._engine.getTransformManager();
      var inst = tcm.getInstance(entity);
      if (!inst) return this;

      var current = tcm.getTransform(inst);
      if (current && current.length === 16) {
        // Extract current column lengths (old scales) and normalize rotation
        var oldSx = Math.sqrt(current[0] * current[0] + current[1] * current[1] + current[2] * current[2]) || 1;
        var oldSy = Math.sqrt(current[4] * current[4] + current[5] * current[5] + current[6] * current[6]) || 1;
        var oldSz = Math.sqrt(current[8] * current[8] + current[9] * current[9] + current[10] * current[10]) || 1;

        // Rescale each column
        current[0] = (current[0] / oldSx) * sx; current[1] = (current[1] / oldSx) * sx; current[2] = (current[2] / oldSx) * sx;
        current[4] = (current[4] / oldSy) * sy; current[5] = (current[5] / oldSy) * sy; current[6] = (current[6] / oldSy) * sy;
        current[8] = (current[8] / oldSz) * sz; current[9] = (current[9] / oldSz) * sz; current[10] = (current[10] / oldSz) * sz;

        tcm.setTransform(inst, current);
      } else {
        tcm.setTransform(inst, _scaleMatrix(sx, sy, sz));
      }
      return this;
    }

    /**
     * Change the color of a procedural geometry entity.
     * @param {number} entity - Filament Entity handle
     * @param {number} r - red 0-1
     * @param {number} g - green 0-1
     * @param {number} b - blue 0-1
     * @param {number} [a=1] - alpha 0-1
     */
    setEntityColor(entity, r, g, b, a) {
      if (a === undefined) a = 1;
      var record = this._findGeometryRecord(entity);
      if (record && record.matInstance) {
        try {
          record.matInstance.setColor4Parameter('baseColor', Filament.RgbaType.sRGB, [r, g, b, a]);
        } catch (e) {
          console.warn('SceneView: setEntityColor failed —', e.message);
        }
      }
      return this;
    }

    /**
     * Remove a procedural geometry entity from the scene and free its resources.
     * @param {number} entity - Filament Entity handle
     */
    removeEntity(entity) {
      if (!entity) return;
      this._scene.remove(entity);

      // Clean up tracked resources
      var idx = -1;
      for (var i = 0; i < this._geometryEntities.length; i++) {
        if (this._geometryEntities[i].entity === entity) {
          idx = i;
          break;
        }
      }
      if (idx >= 0) {
        var record = this._geometryEntities[idx];
        try {
          this._engine.destroyEntity(entity);
        } catch (e) { /* ignore */ }
        this._geometryEntities.splice(idx, 1);
      } else {
        // Not a tracked geometry entity, just remove from scene
        try {
          this._engine.destroyEntity(entity);
        } catch (e) { /* ignore */ }
      }
    }

    /** @private Find tracked geometry record by entity */
    _findGeometryRecord(entity) {
      for (var i = 0; i < this._geometryEntities.length; i++) {
        if (this._geometryEntities[i].entity === entity) {
          return this._geometryEntities[i];
        }
      }
      return null;
    }

    // -----------------------------------------------------------------------
    // Camera & scene settings (existing API)
    // -----------------------------------------------------------------------

    setAutoRotate(enabled) { this._autoRotate = enabled; this._wantsAutoRotate = enabled; return this; }
    setCameraDistance(d) { this._orbitRadius = d; return this; }

    setBackgroundColor(r, g, b, a) {
      this._renderer.setClearOptions({ clearColor: [r, g, b, a !== undefined ? a : 1], clear: true });
      return this;
    }

    /** Access engine for advanced Filament operations */
    get engine() { return this._engine; }
    get scene() { return this._scene; }

    dispose() {
      this._running = false;
      if (this._resizeObserver) this._resizeObserver.disconnect();
      // Clean up geometry entities
      for (var i = 0; i < this._geometryEntities.length; i++) {
        try { this._engine.destroyEntity(this._geometryEntities[i].entity); } catch (e) { /* skip */ }
      }
      this._geometryEntities = [];
      try { Filament.Engine.destroy(this._engine); } catch (e) { /* already destroyed */ }
    }

    // -----------------------------------------------------------------------
    // Controls, resize, render loop (unchanged from original)
    // -----------------------------------------------------------------------

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

        if (self._autoRotate) self._angle += 0.00873;

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
          console.error('SceneView render error:', e.message);
          self._running = false;
        }
        requestAnimationFrame(render);
      }
      render();
    }
  }

  // =========================================================================
  // Engine creation
  // =========================================================================

  var _activeCanvases = new Set();

  function _createEngine(canvasOrId, options) {
    options = options || {};

    var canvas = typeof canvasOrId === 'string'
      ? document.getElementById(canvasOrId)
      : canvasOrId;
    if (!canvas) throw new Error('Canvas not found: ' + canvasOrId);

    if (_activeCanvases.has(canvas)) {
      console.warn('SceneView: Canvas already initialized, skipping');
      return null;
    }
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

    // Post-processing
    try {
      view.setAmbientOcclusionOptions({
        enabled: true, radius: 0.3, bias: 0.0005, intensity: 1.0, quality: 1
      });
    } catch (e) { /* skip */ }

    // 3-point studio lighting
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

    var fill = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
      .color([0.7, 0.75, 0.9])
      .intensity(60000)
      .direction([-0.5, 0.5, 1.0])
      .build(engine, fill);
    scene.addEntity(fill);

    var back = Filament.EntityManager.get().create();
    Filament.LightManager.Builder(Filament.LightManager$Type.DIRECTIONAL)
      .color([0.5, 0.6, 0.9])
      .intensity(50000)
      .direction([0, 0.3, 1.0])
      .build(engine, back);
    scene.addEntity(back);

    // IBL
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
                var skybox = Filament.Skybox.Builder()
                  .environment(reflections)
                  .build(engine);
                scene.setSkybox(skybox);
              }
            } catch (skyErr) { /* skip */ }
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

  // =========================================================================
  // Public API
  // =========================================================================

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

  /**
   * Set the base path for material files (.filamat).
   * Default is auto-detected from the sceneview.js script tag location.
   * @param {string} path - e.g. '/assets/materials/' or 'materials/'
   */
  function setMaterialsPath(path) {
    _materialsBasePath = path;
    if (path[path.length - 1] !== '/') _materialsBasePath += '/';
  }

  global.SceneView = {
    version: '2.0.0',
    create: create,
    modelViewer: modelViewer,
    setMaterialsPath: setMaterialsPath
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
