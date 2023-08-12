package io.github.sceneview.sample.ecommerce.productdescription.data

data class Product(
    val images: List<String>,
    val description: String,
    val title: String,
    val color: String,
    val priceInCents: Int
)