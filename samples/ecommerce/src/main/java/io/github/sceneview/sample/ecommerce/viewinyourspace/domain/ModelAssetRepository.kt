package io.github.sceneview.sample.ecommerce.viewinyourspace.domain

interface ModelAssetRepository {
    suspend fun fetchAsset(productId: Int): String
}