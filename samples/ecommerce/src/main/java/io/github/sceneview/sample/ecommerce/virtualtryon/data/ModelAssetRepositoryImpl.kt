package io.github.sceneview.sample.ecommerce.virtualtryon.data

import io.github.sceneview.sample.ecommerce.virtualtryon.domain.ModelAssetRepository
import kotlinx.coroutines.delay

class ModelAssetRepositoryImpl : ModelAssetRepository{
    override suspend fun fetchAsset(productId: Int) : String {
        // return some data
        delay(1000)
        return "https://storage.googleapis.com/ar-answers-in-search-models/static/GiantPanda/model.glb"
    }
}