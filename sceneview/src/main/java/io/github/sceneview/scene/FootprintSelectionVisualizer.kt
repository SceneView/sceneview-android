/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.sceneview.scene

import com.google.ar.sceneform.rendering.ModelRenderable
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

/**
 * Visualizes that a [Node] is selected by rendering a footprint for the
 * selected node.
 */
class FootprintSelectionVisualizer : SelectionVisualizer {
    private val footprintNode: ModelNode = ModelNode()
    var footprintRenderable: ModelRenderable? = null
        set(newValue) {
            val copyRenderable = newValue?.makeCopy()
            copyRenderable?.let {
                footprintNode.setModel(it)
                copyRenderable.collisionShape = null
            }
            field = copyRenderable
        }

    override fun applySelectionVisual(node: Node) {
        footprintNode.parent = node
    }

    override fun removeSelectionVisual(node: Node) {
        footprintNode.parent = null
    }
}