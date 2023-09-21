package io.github.sceneview.sample.ecommerce.viewinyourspace.data

import io.github.sceneview.sample.ecommerce.viewinyourspace.domain.ModelAssetRepository
import kotlinx.coroutines.delay

class ModelAssetRepositoryImpl : ModelAssetRepository{
    override suspend fun fetchAsset(productId: Int) : String {
        // return some data
        delay(2000)
        return "models/orangehandbag.glb"
    }
}