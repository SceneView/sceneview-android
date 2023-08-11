package io.github.sceneview.sample.ecommerce.virtualtryon.domain

interface ModelAssetRepository {
    suspend fun fetchAsset(productId: Int): String
}