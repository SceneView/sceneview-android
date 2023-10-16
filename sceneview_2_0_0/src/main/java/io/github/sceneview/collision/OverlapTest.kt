package io.github.sceneview.collision

//TODO
///**
// * Tests to see if the given node's collision shape overlaps the collision shape of any other
// * nodes in the scene using [Node.getCollisionShape]. The node used for testing does not
// * need to be active.
// *
// * @param node The node to use for the test.
// * @return A node that is overlapping the test node. If no node is overlapping the test node, then
// * this is null. If multiple nodes are overlapping the test node, then this could be any of
// * them.
// */
//fun SceneView.overlapTest(node: Node) = node.collider?.let { collider ->
//    collisionSystem.intersects(collider)?.let { intersectedCollider ->
//        intersectedCollider.transformProvider as Node
//    }
//}
//
///**
// * Tests to see if a node is overlapping any other nodes within the scene using
// * [Node.getCollisionShape].
// * The node used for testing does not need to be active.
// *
// * @param node The node to use for the test.
// * @return A list of all nodes that are overlapping the test node. If no node is overlapping the
// * test node, then the list is empty.
// */
//fun SceneView.overlapTestAll(node: Node): ArrayList<Node> = ArrayList<Node>().apply {
//    node.collider?.let { collider ->
//        collisionSystem.intersectsAll(collider) { intersectedCollider: Collider ->
//            add(intersectedCollider.transformProvider as Node)
//        }
//    }
//}
