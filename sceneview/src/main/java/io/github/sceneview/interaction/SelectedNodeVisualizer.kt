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
package io.github.sceneview.interaction

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

const val defaultNodeSelector = "sceneview/models/node_selector.glb"

/**
 * ### Visualizes that a [Node] is selected by rendering a footprint for the selected node
 */
class SelectedNodeVisualizer(
    context: Context,
    val lifecycle: Lifecycle,
    nodeSelectorModel: String = defaultNodeSelector
) : SceneLifecycleObserver {

    private val selectorNode: ModelNode = ModelNode()

    init {
        lifecycle.addObserver(this)

        lifecycle.coroutineScope.launchWhenCreated {
            selectorNode.loadModel(context = context, lifecycle, nodeSelectorModel)
            selectorNode.collisionShape = null
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        selectorNode.destroy()
        super.onDestroy(owner)
    }

    fun selectNode(node: Node, selected: Boolean) {
        selectorNode.parent = if (selected) node else null
    }
}
