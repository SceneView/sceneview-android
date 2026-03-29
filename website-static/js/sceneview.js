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
      // Animation system state
      this._animator = null;
      this._animationStates = [];  // Per-animation: { playing, paused, time, speed, loop, weight }
      this._animationCallbacks = { onEnd: {}, onLoop: {} };  // Keyed by animation index
      this._lastFrameTime = performance.now() / 1000;
      this._crossFades = [];  // Active cross-fade transitions
      this._morphAnimations = [];  // Active morph target animations
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

      // Initialize animation system for this asset
      this._initAnimations(asset);

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

    // =========================================================================
    // Animation System
    // =========================================================================

    /**
     * Initialize animation state from the loaded glTF asset.
     * Called automatically after loadModel / _showModel.
     */
    _initAnimations(asset) {
      this._animator = null;
      this._animationStates = [];
      this._animationCallbacks = { onEnd: {}, onLoop: {} };
      this._crossFades = [];
      this._morphAnimations = [];

      try {
        // Filament.js: the animator lives on the asset instance
        var animator = asset.getAnimator();
        if (!animator) return;
        this._animator = animator;

        var count = animator.getAnimationCount();
        for (var i = 0; i < count; i++) {
          this._animationStates.push({
            name: animator.getAnimationName(i) || ('Animation_' + i),
            duration: animator.getAnimationDuration(i) || 0,
            index: i,
            playing: false,
            paused: false,
            time: 0,
            speed: 1.0,
            loop: false,
            weight: 1.0
          });
        }
      } catch (e) {
        // Asset has no animations — that is fine
        console.log('SceneView: No animations found in model');
      }
    }

    /**
     * Resolve a name-or-index argument to an animation index.
     * Returns -1 if not found.
     */
    _resolveAnimationIndex(nameOrIndex) {
      if (typeof nameOrIndex === 'number') {
        return (nameOrIndex >= 0 && nameOrIndex < this._animationStates.length) ? nameOrIndex : -1;
      }
      for (var i = 0; i < this._animationStates.length; i++) {
        if (this._animationStates[i].name === nameOrIndex) return i;
      }
      return -1;
    }

    // --- Animation Discovery ---

    /** Get all animations as an array of { name, duration, index }. */
    getAnimations() {
      return this._animationStates.map(function(s) {
        return { name: s.name, duration: s.duration, index: s.index };
      });
    }

    /** Get the number of animations in the current model. */
    getAnimationCount() {
      return this._animationStates.length;
    }

    /** Get the name of animation at the given index. */
    getAnimationName(index) {
      var s = this._animationStates[index];
      return s ? s.name : null;
    }

    /** Get the duration in seconds of animation at the given index. */
    getAnimationDuration(index) {
      var s = this._animationStates[index];
      return s ? s.duration : 0;
    }

    // --- Animation Playback ---

    /**
     * Play an animation by name or index.
     * @param {string|number} nameOrIndex - Animation name or index.
     * @param {Object} [options] - Playback options.
     * @param {boolean} [options.loop=false] - Loop the animation.
     * @param {number} [options.speed=1.0] - Playback speed (negative = reverse).
     * @param {number} [options.crossFadeDuration=0] - Cross-fade from current animation (seconds).
     * @param {number} [options.startTime=0] - Start time in seconds.
     * @returns {SceneViewInstance} this (for chaining)
     */
    playAnimation(nameOrIndex, options) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) {
        console.warn('SceneView: Animation not found:', nameOrIndex);
        return this;
      }

      options = options || {};
      var state = this._animationStates[idx];
      var crossFadeDuration = options.crossFadeDuration || 0;

      // If cross-fading, set up a cross-fade from currently playing animations
      if (crossFadeDuration > 0) {
        for (var i = 0; i < this._animationStates.length; i++) {
          if (i !== idx && this._animationStates[i].playing) {
            this._crossFades.push({
              fromIndex: i,
              toIndex: idx,
              duration: crossFadeDuration,
              elapsed: 0,
              fromStartWeight: this._animationStates[i].weight,
              toStartWeight: 0
            });
          }
        }
        // New animation starts at weight 0, cross-fade will ramp it up
        state.weight = crossFadeDuration > 0 ? 0 : 1.0;
      } else {
        state.weight = 1.0;
      }

      var wasPaused = state.paused;
      state.playing = true;
      state.paused = false;
      state.loop = options.loop !== undefined ? options.loop : state.loop;
      state.speed = options.speed !== undefined ? options.speed : state.speed;
      if (options.startTime !== undefined) {
        state.time = options.startTime;
      } else if (!wasPaused) {
        // Only reset time if not resuming from pause
        state.time = state.speed >= 0 ? 0 : state.duration;
      }

      return this;
    }

    /**
     * Play all animations simultaneously.
     * @param {Object} [options] - Same options as playAnimation.
     * @returns {SceneViewInstance} this
     */
    playAllAnimations(options) {
      for (var i = 0; i < this._animationStates.length; i++) {
        this.playAnimation(i, options);
      }
      return this;
    }

    /**
     * Pause an animation. Can be resumed with playAnimation.
     * @param {string|number} nameOrIndex
     * @returns {SceneViewInstance} this
     */
    pauseAnimation(nameOrIndex) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      var state = this._animationStates[idx];
      if (state.playing) {
        state.playing = false;
        state.paused = true;
      }
      return this;
    }

    /** Pause all animations. */
    pauseAllAnimations() {
      for (var i = 0; i < this._animationStates.length; i++) {
        this.pauseAnimation(i);
      }
      return this;
    }

    /**
     * Stop an animation and reset its time to 0.
     * @param {string|number} nameOrIndex
     * @returns {SceneViewInstance} this
     */
    stopAnimation(nameOrIndex) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      var state = this._animationStates[idx];
      state.playing = false;
      state.paused = false;
      state.time = 0;
      state.weight = 1.0;
      // Remove any cross-fades involving this animation
      this._crossFades = this._crossFades.filter(function(cf) {
        return cf.fromIndex !== idx && cf.toIndex !== idx;
      });
      return this;
    }

    /** Stop all animations and reset to time 0. */
    stopAllAnimations() {
      for (var i = 0; i < this._animationStates.length; i++) {
        this._animationStates[i].playing = false;
        this._animationStates[i].paused = false;
        this._animationStates[i].time = 0;
        this._animationStates[i].weight = 1.0;
      }
      this._crossFades = [];
      return this;
    }

    /**
     * Set the playback speed for an animation.
     * @param {string|number} nameOrIndex
     * @param {number} speed - 1.0 = normal, 2.0 = double, -1.0 = reverse.
     * @returns {SceneViewInstance} this
     */
    setAnimationSpeed(nameOrIndex, speed) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      this._animationStates[idx].speed = speed;
      return this;
    }

    /**
     * Seek an animation to a specific time.
     * @param {string|number} nameOrIndex
     * @param {number} time - Time in seconds.
     * @returns {SceneViewInstance} this
     */
    setAnimationTime(nameOrIndex, time) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      var state = this._animationStates[idx];
      state.time = Math.max(0, Math.min(time, state.duration));
      return this;
    }

    // --- Animation State ---

    /**
     * Check if an animation is currently playing (not paused, not stopped).
     * @param {string|number} nameOrIndex
     * @returns {boolean}
     */
    isAnimationPlaying(nameOrIndex) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return false;
      return this._animationStates[idx].playing;
    }

    /**
     * Get the progress of an animation (0.0 to 1.0).
     * @param {string|number} nameOrIndex
     * @returns {number} Progress from 0.0 to 1.0.
     */
    getAnimationProgress(nameOrIndex) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return 0;
      var state = this._animationStates[idx];
      if (state.duration <= 0) return 0;
      return Math.max(0, Math.min(1, state.time / state.duration));
    }

    // --- Morph Targets (Blend Shapes) ---

    /**
     * Get the morph target names for an entity in the current asset.
     * If no entity is specified, uses the first renderable entity.
     * @param {number} [entity] - Filament entity. Omit to use first renderable.
     * @returns {string[]} Array of morph target names.
     */
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
        for (var i = 0; i < count; i++) {
          names.push(rm.getMorphTargetNameAt(inst, 0, i) || ('morph_' + i));
        }
        return names;
      } catch (e) {
        return [];
      }
    }

    /**
     * Set a morph target weight on an entity.
     * @param {number} [entity] - Filament entity. Omit to use first renderable.
     * @param {string|number} name - Morph target name or index.
     * @param {number} weight - Weight from 0.0 to 1.0.
     * @returns {SceneViewInstance} this
     */
    setMorphTargetWeight(entity, name, weight) {
      if (!this._asset) return this;
      try {
        var targetEntity = entity;
        // If called with 2 args (name, weight), shift params
        if (weight === undefined && typeof name === 'number') {
          weight = name;
          name = entity;
          targetEntity = null;
        }
        if (targetEntity === undefined || targetEntity === null) {
          var renderables = this._asset.getRenderableEntities();
          if (!renderables || renderables.length === 0) return this;
          targetEntity = renderables[0];
        }

        var rm = this._engine.getRenderableManager();
        var inst = rm.getInstance(targetEntity);
        var morphIndex = typeof name === 'number' ? name : -1;

        // Resolve name to index
        if (typeof name === 'string') {
          var count = rm.getMorphTargetCountAt(inst, 0);
          for (var i = 0; i < count; i++) {
            if (rm.getMorphTargetNameAt(inst, 0, i) === name) {
              morphIndex = i;
              break;
            }
          }
        }

        if (morphIndex < 0) {
          console.warn('SceneView: Morph target not found:', name);
          return this;
        }

        // Clamp weight
        weight = Math.max(0, Math.min(1, weight));

        // Filament uses setMorphWeights with a float array
        // We need to get the current weights, modify one, and set them all
        var totalCount = rm.getMorphTargetCountAt(inst, 0);
        var weights = new Float32Array(totalCount);
        // Get existing weights if possible
        try {
          var existing = rm.getMorphWeights(inst, 0);
          if (existing) {
            for (var j = 0; j < totalCount; j++) weights[j] = existing[j] || 0;
          }
        } catch (e) { /* start from zeros */ }
        weights[morphIndex] = weight;
        rm.setMorphWeights(inst, weights, 0);
      } catch (e) {
        console.warn('SceneView: Failed to set morph target weight:', e.message);
      }
      return this;
    }

    /**
     * Animate a morph target weight over time.
     * @param {number} [entity] - Filament entity. Omit to use first renderable.
     * @param {string|number} name - Morph target name or index.
     * @param {number} fromWeight - Start weight (0.0-1.0).
     * @param {number} toWeight - End weight (0.0-1.0).
     * @param {number} duration - Duration in seconds.
     * @returns {Promise} Resolves when animation completes.
     */
    animateMorphTarget(entity, name, fromWeight, toWeight, duration) {
      var self = this;
      // Handle overloaded args: (name, from, to, duration) with no entity
      if (typeof entity === 'string' || (typeof entity === 'number' && fromWeight !== undefined && toWeight !== undefined && duration !== undefined)) {
        // All 5 args provided, entity is actually an entity
      } else if (typeof entity === 'string' && typeof name === 'number') {
        // (name, fromWeight, toWeight, duration) — shift args
        duration = toWeight;
        toWeight = fromWeight;
        fromWeight = name;
        name = entity;
        entity = null;
      }

      return new Promise(function(resolve) {
        self._morphAnimations.push({
          entity: entity,
          name: name,
          fromWeight: fromWeight,
          toWeight: toWeight,
          duration: duration,
          elapsed: 0,
          resolve: resolve
        });
      });
    }

    // --- Animation Events ---

    /**
     * Register a callback for when an animation ends (non-looping only).
     * @param {string|number} nameOrIndex
     * @param {Function} callback - Called with { name, index, time }.
     * @returns {SceneViewInstance} this
     */
    onAnimationEnd(nameOrIndex, callback) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      if (!this._animationCallbacks.onEnd[idx]) {
        this._animationCallbacks.onEnd[idx] = [];
      }
      this._animationCallbacks.onEnd[idx].push(callback);
      return this;
    }

    /**
     * Register a callback for when a looping animation completes one cycle.
     * @param {string|number} nameOrIndex
     * @param {Function} callback - Called with { name, index, loopCount }.
     * @returns {SceneViewInstance} this
     */
    onAnimationLoop(nameOrIndex, callback) {
      var idx = this._resolveAnimationIndex(nameOrIndex);
      if (idx < 0) return this;
      if (!this._animationCallbacks.onLoop[idx]) {
        this._animationCallbacks.onLoop[idx] = [];
      }
      this._animationCallbacks.onLoop[idx].push(callback);
      return this;
    }

    /**
     * Update all active animations for the current frame.
     * Called from the render loop.
     * @param {number} deltaTime - Time elapsed since last frame in seconds.
     */
    _updateAnimations(deltaTime) {
      // Always update morph animations, even without a glTF animator
      this._updateMorphAnimations(deltaTime);

      if (!this._animator || this._animationStates.length === 0) return;

      var animator = this._animator;
      var hasActiveAnimation = false;

      // Update cross-fades
      for (var cf = this._crossFades.length - 1; cf >= 0; cf--) {
        var fade = this._crossFades[cf];
        fade.elapsed += deltaTime;
        var t = Math.min(1, fade.elapsed / fade.duration);
        // Smooth interpolation (ease in-out)
        t = t * t * (3 - 2 * t);

        if (fade.fromIndex < this._animationStates.length) {
          this._animationStates[fade.fromIndex].weight = fade.fromStartWeight * (1 - t);
        }
        if (fade.toIndex < this._animationStates.length) {
          this._animationStates[fade.toIndex].weight = t;
        }

        if (fade.elapsed >= fade.duration) {
          // Cross-fade complete: stop the "from" animation
          if (fade.fromIndex < this._animationStates.length) {
            this._animationStates[fade.fromIndex].playing = false;
            this._animationStates[fade.fromIndex].weight = 0;
          }
          if (fade.toIndex < this._animationStates.length) {
            this._animationStates[fade.toIndex].weight = 1.0;
          }
          this._crossFades.splice(cf, 1);
        }
      }

      // Reset the animator bone transforms before applying weighted animations
      try { animator.resetBoneMatrices(); } catch (e) { /* not all versions support this */ }

      // Update each animation's time and apply
      for (var i = 0; i < this._animationStates.length; i++) {
        var state = this._animationStates[i];
        if (!state.playing) continue;

        hasActiveAnimation = true;

        // Advance time
        state.time += deltaTime * state.speed;

        // Handle loop / end
        if (state.speed >= 0 && state.time >= state.duration) {
          if (state.loop) {
            // Loop: wrap around
            var loopCount = Math.floor(state.time / state.duration);
            state.time = state.time % state.duration;
            // Fire loop callbacks
            this._fireCallbacks('onLoop', i, { name: state.name, index: i, loopCount: loopCount });
          } else {
            // End: clamp and stop
            state.time = state.duration;
            state.playing = false;
            state.paused = false;
            this._fireCallbacks('onEnd', i, { name: state.name, index: i, time: state.time });
          }
        } else if (state.speed < 0 && state.time <= 0) {
          if (state.loop) {
            var loopCountRev = Math.floor(Math.abs(state.time) / state.duration) + 1;
            state.time = state.duration - (Math.abs(state.time) % state.duration);
            this._fireCallbacks('onLoop', i, { name: state.name, index: i, loopCount: loopCountRev });
          } else {
            state.time = 0;
            state.playing = false;
            state.paused = false;
            this._fireCallbacks('onEnd', i, { name: state.name, index: i, time: 0 });
          }
        }

        // Apply the animation at the current time with weight
        var clampedTime = Math.max(0, Math.min(state.time, state.duration));
        try {
          if (state.weight > 0) {
            animator.applyAnimation(i, clampedTime);
            // If we have weighted blending, apply cross-fade weight
            if (state.weight < 1.0) {
              try { animator.applyCrossFade(i, clampedTime, state.weight); } catch (e) {
                // applyCrossFade may not exist in all Filament.js builds;
                // fall back to basic applyAnimation (last-wins blending)
              }
            }
          }
        } catch (e) {
          console.warn('SceneView: Failed to apply animation ' + i + ':', e.message);
          state.playing = false;
        }
      }

      // Tell Filament to update the bone matrices
      if (hasActiveAnimation) {
        try { animator.updateBoneMatrices(); } catch (e) { /* ignore */ }
      }
    }

    /**
     * Fire registered callbacks for an event type.
     * @param {string} type - 'onEnd' or 'onLoop'.
     * @param {number} index - Animation index.
     * @param {Object} data - Event data to pass to callbacks.
     */
    _fireCallbacks(type, index, data) {
      var callbacks = this._animationCallbacks[type][index];
      if (!callbacks) return;
      for (var i = 0; i < callbacks.length; i++) {
        try { callbacks[i](data); } catch (e) {
          console.error('SceneView: Animation callback error:', e);
        }
      }
    }

    /**
     * Update morph target animations independently of glTF skeleton animations.
     * @param {number} deltaTime
     */
    _updateMorphAnimations(deltaTime) {
      for (var m = this._morphAnimations.length - 1; m >= 0; m--) {
        var morph = this._morphAnimations[m];
        morph.elapsed += deltaTime;
        var progress = Math.min(1, morph.elapsed / morph.duration);
        // Smooth step interpolation
        var smoothT = progress * progress * (3 - 2 * progress);
        var currentWeight = morph.fromWeight + (morph.toWeight - morph.fromWeight) * smoothT;
        this.setMorphTargetWeight(morph.entity, morph.name, currentWeight);

        if (morph.elapsed >= morph.duration) {
          this.setMorphTargetWeight(morph.entity, morph.name, morph.toWeight);
          this._morphAnimations.splice(m, 1);
          if (morph.resolve) morph.resolve();
        }
      }
    }

    /** Access engine for advanced Filament operations */
    get engine() { return this._engine; }
    get scene() { return this._scene; }

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

        // Compute delta time for animations
        var now = performance.now() / 1000;
        var deltaTime = Math.min(now - self._lastFrameTime, 0.1); // Cap at 100ms to avoid jumps
        self._lastFrameTime = now;

        // Update animations before rendering
        self._updateAnimations(deltaTime);

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

  global.SceneView = {
    version: '1.5.0',
    create: create,
    modelViewer: modelViewer
  };

})(typeof globalThis !== 'undefined' ? globalThis : window);
