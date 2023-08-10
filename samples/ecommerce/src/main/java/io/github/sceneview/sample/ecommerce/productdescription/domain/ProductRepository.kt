package io.github.sceneview.sample.ecommerce.productdescription.domain

import io.github.sceneview.sample.ecommerce.productdescription.data.Product

interface ProductRepository{
    suspend fun fetchProductMetadata(id: Int): Product
}