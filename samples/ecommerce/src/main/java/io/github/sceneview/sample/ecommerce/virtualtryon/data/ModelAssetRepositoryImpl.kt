package io.github.sceneview.sample.ecommerce.virtualtryon.data

import io.github.sceneview.sample.ecommerce.virtualtryon.domain.ModelAssetRepository
import kotlinx.coroutines.delay

class ModelAssetRepositoryImpl : ModelAssetRepository{
    override suspend fun fetchAsset(productId: Int) : String {
        // return some data
        delay(2000)
        return "models/orangehandbag.glb"
    }
}