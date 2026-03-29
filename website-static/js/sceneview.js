/**
 * SceneView Web — Simple 3D for the web.
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
  // Math utilities for 4x4 matrix operations (column-major, Float32Array[16])
  // =========================================================================

  var _mat4 = {
    /** Return a new identity matrix */
    identity: function() {
      return new Float32Array([
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
      ]);
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

    /** Build a TRS matrix from position [x,y,z], euler rotation [rx,ry,rz] in degrees, scale [sx,sy,sz] */
    fromTRS: function(position, rotation, scale) {
      var px = position[0], py = position[1], pz = position[2];
      var sx = scale[0], sy = scale[1], sz = scale[2];

      // Euler angles (degrees) to radians — rotation order: Y * X * Z (standard for 3D scenes)
      var DEG2RAD = Math.PI / 180;
      var rx = rotation[0] * DEG2RAD;
      var ry = rotation[1] * DEG2RAD;
      var rz = rotation[2] * DEG2RAD;

      var cx = Math.cos(rx), sx_ = Math.sin(rx);
      var cy = Math.cos(ry), sy_ = Math.sin(ry);
      var cz = Math.cos(rz), sz_ = Math.sin(rz);

      // Combined rotation matrix R = Ry * Rx * Rz
      var r00 = cy * cz + sy_ * sx_ * sz_;
      var r01 = cx * sz_;
      var r02 = -sy_ * cz + cy * sx_ * sz_;
      var r10 = cy * -sz_ + sy_ * sx_ * cz;
      var r11 = cx * cz;
      var r12 = sy_ * sz_ + cy * sx_ * cz;
      var r20 = sy_ * cx;
      var r21 = -sx_;
      var r22 = cy * cx;

      // Column-major TRS matrix
      return new Float32Array([
        r00 * sx, r01 * sx, r02 * sx, 0,
        r10 * sy, r11 * sy, r12 * sy, 0,
        r20 * sz, r21 * sz, r22 * sz, 0,
        px,       py,       pz,       1
      ]);
    },

    /** Extract translation from a 4x4 column-major matrix */
    getTranslation: function(m) {
      return [m[12], m[13], m[14]];
    },

    /** Extract scale from a 4x4 column-major matrix (length of each column) */
    getScale: function(m) {
      return [
        Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]),
        Math.sqrt(m[4] * m[4] + m[5] * m[5] + m[6] * m[6]),
        Math.sqrt(m[8] * m[8] + m[9] * m[9] + m[10] * m[10])
      ];
    },

    /** Extract euler angles (degrees) from a 4x4 column-major matrix — assumes Y*X*Z order */
    getRotation: function(m) {
      var scale = _mat4.getScale(m);
      var invSx = scale[0] !== 0 ? 1 / scale[0] : 0;
      var invSy = scale[1] !== 0 ? 1 / scale[1] : 0;
      var invSz = scale[2] !== 0 ? 1 / scale[2] : 0;

      // Normalized rotation matrix elements
      var r20 = m[8] * invSz;
      var r21 = m[9] * invSz;
      var r22 = m[10] * invSz;
      var r00 = m[0] * invSx;
      var r11 = m[5] * invSy;

      var RAD2DEG = 180 / Math.PI;
      var rx = Math.asin(-Math.max(-1, Math.min(1, r21))) * RAD2DEG;
      var ry = Math.atan2(r20, r22) * RAD2DEG;
      var rz = Math.atan2(r00 !== 0 ? m[1] * invSx : 0, r11) * RAD2DEG;
      return [rx, ry, rz];
    }
  };

  // =========================================================================
  // SceneNode — base class for all scene graph nodes
  // =========================================================================

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
  // SceneViewInstance — wraps Filament engine, scene, camera, renderer
  // =========================================================================

  /**
   * SceneView instance — wraps Filament engine, scene, camera, renderer.
   * Now includes a scene graph with a root node and declarative builder.
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

      // Scene graph
      this._root = new GroupNode('root');
      this._root._setSceneInstance(this);

      this._setupControls();
      this._setupResizeObserver();
      this._startRenderLoop();
    }

    // --- Scene graph access ---

    /** @returns {GroupNode} The root node of the scene graph */
    get root() { return this._root; }

    /**
     * Declarative scene builder. Pass a callback that receives a SceneBuilder
     * for the root node.
     *
     * @example
     * scene.build(root => {
     *   root.model('robot.glb', { position: [0, 0, 0] })
     *   root.light('point', { position: [2, 3, 0], intensity: 10000 })
     *   root.group('furniture', group => {
     *     group.model('chair.glb', { position: [1, 0, 0] })
     *     group.model('table.glb', { position: [0, 0, 1] })
     *   })
     *   root.cube({ size: [1,1,1], position: [3, 0, 0], color: [1, 0, 0] })
     * })
     *
     * @param {function(SceneBuilder): void} callback
     * @returns {SceneViewInstance} this (for chaining)
     */
    build(callback) {
      var builder = new SceneBuilder(this._root, this);
      callback(builder);
      return this;
    }

    /**
     * Search the entire scene graph for a node by name.
     * @param {string} name
     * @returns {SceneNode|null}
     */
    findByName(name) {
      return this._root.findByName(name);
    }

    /**
     * Clear all nodes from the scene graph (except the root).
     * Destroys all child nodes and their Filament resources.
     */
    clear() {
      var children = this._root.children;
      for (var i = 0; i < children.length; i++) {
        children[i].destroy();
      }
    }

    /**
     * Depth-first traversal of the entire scene graph.
     * @param {function(SceneNode, number): void} callback - Receives (node, depth)
     */
    traverse(callback) {
      this._root.traverse(callback);
    }

    // --- Convenience methods that return SceneNode wrappers ---

    /**
     * Add a model to the scene. Returns a ModelNode.
     * @param {string} url - Model URL (.glb/.gltf)
     * @param {object} [options] - position, rotation, scale, name, etc.
     * @returns {Promise<ModelNode>}
     */
    addModel(url, options) {
      options = options || {};
      var name = options.name || url.split('/').pop().split('.')[0];
      var node = new ModelNode(name, options);
      this._root.addChild(node);
      node._setSceneInstance(this);
      return node.load(url);
    }

    /**
     * Add a light to the scene. Returns a LightNode.
     * @param {string} lightType - 'point', 'spot', 'directional', 'sun'
     * @param {object} [options] - Light options (color, intensity, direction, position, etc.)
     * @returns {LightNode}
     */
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

    /**
     * Add a cube geometry node. Returns a GeometryNode.
     * @param {object} [options] - size, color, position, etc.
     * @returns {GeometryNode}
     */
    addCube(options) { return this._addGeometry('cube', options); }

    /**
     * Add a sphere geometry node. Returns a GeometryNode.
     * @param {object} [options] - size (radius), color, segments, position, etc.
     * @returns {GeometryNode}
     */
    addSphere(options) { return this._addGeometry('sphere', options); }

    /**
     * Add a cylinder geometry node. Returns a GeometryNode.
     * @param {object} [options] - size ([radius, height]), color, segments, position, etc.
     * @returns {GeometryNode}
     */
    addCylinder(options) { return this._addGeometry('cylinder', options); }

    /**
     * Add a plane geometry node. Returns a GeometryNode.
     * @param {object} [options] - size ([width, height]), color, position, etc.
     * @returns {GeometryNode}
     */
    addPlane(options) { return this._addGeometry('plane', options); }

    /**
     * Add a group node. Returns a GroupNode.
     * @param {string} [name] - Group name
     * @param {object} [options] - position, rotation, scale
     * @returns {GroupNode}
     */
    addGroup(name, options) {
      var node = new GroupNode(name || 'group', options);
      this._root.addChild(node);
      node._setSceneInstance(this);
      return node;
    }

    /** @private */
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

    // --- Legacy API (backward compatible) ---

    /** Load a glTF/GLB model from URL (legacy API, still works) */
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

      // Destroy scene graph
      this.clear();

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

        // Update scene graph transforms (apply world matrices to Filament entities)
        self._updateSceneGraph();

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

    /**
     * Walk the scene graph and apply transforms to Filament entities.
     * Called once per frame in the render loop.
     * @private
     */
    _updateSceneGraph() {
      this._root.traverse(function(node) {
        if (node._entity && node._worldMatrixDirty) {
          node._applyTransformToFilament();
        }
      });
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

  // =========================================================================
  // Public API
  // =========================================================================

  global.SceneView = {
    version: '2.0.0',
    create: create,
    modelViewer: modelViewer,

    // Node classes (for advanced usage, instanceof checks, or extending)
    SceneNode: SceneNode,
    ModelNode: ModelNode,
    LightNode: LightNode,
    GeometryNode: GeometryNode,
    GroupNode: GroupNode
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
