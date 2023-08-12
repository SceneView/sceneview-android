package io.github.sceneview.sample.ecommerce.productdescription.data

import io.github.sceneview.sample.ecommerce.productdescription.domain.ProductRepository

class ProductRepositoryImpl : ProductRepository {
    override suspend fun fetchProductMetadata(id: Int): Product {
         return Product(
            images = listOf(
                "https://www.longchamp.com/dw/image/v2/BCVX_PRD/on/demandware.static/-/Sites-LC-master-catalog/default/dwa66395c1/images/DIS/10095HQS504_0.png?sw=800&sh=800&sm=fit",
                "https://www.longchamp.com/dw/image/v2/BCVX_PRD/on/demandware.static/-/Sites-LC-master-catalog/default/dw63a629da/images/DIS/10095HQS504_1.png?sw=800&sh=800&sm=fit"
            ),
            description = "Detachable shoulder strap, Bamboo clasp, Feet on the bottom, 1 zipped pocket and 1 flat pocket inside",
            title = "ROSEAU S HANDBAG",
            color = "Cognac - Leather",
            priceInCents = 98000
        )
    }
}