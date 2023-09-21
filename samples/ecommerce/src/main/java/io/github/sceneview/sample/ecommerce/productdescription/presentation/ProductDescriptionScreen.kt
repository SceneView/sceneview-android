package io.github.sceneview.sample.ecommerce.productdescription.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import io.github.sceneview.sample.ecommerce.R
import java.math.BigDecimal

@Composable
fun ProductDescriptionScreen(
    productId: Int,
    navController: NavHostController,
    productViewModel: ProductDescriptionViewModel
) {

    LaunchedEffect(Unit) {
        productViewModel.dispatchEvent(ProductDescriptionUiEvent.FetchProductData(productId))
    }
    val context = LocalContext.current
    val viewState by productViewModel.state.collectAsState()
    val uiAction by productViewModel.uiAction.collectAsState()

    when (uiAction) {
        // UI actions must be run once, thus run them in a Launched Effect
        is ProductDescriptionUIAction.NavigateToAddToCartScreen -> {
            LaunchedEffect(Unit) {
                Toast.makeText(context, context.getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show()
                productViewModel.onConsumeUIAction()
            }
        }
        is ProductDescriptionUIAction.NavigateToVirtualTryOnScreen -> {
            LaunchedEffect(Unit) {
                navController.navigate("virtual_try_on/$productId")
                productViewModel.onConsumeUIAction()
            }
        }
        null -> {}
    }
    viewState.product?.let { product ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                Modifier.verticalScroll(rememberScrollState())
            ) {
                ImageCarousel(product.images)
                Button(
                    onClick = {
                        // If you need the full product data class, extend Product data class to Parceable interface
                        productViewModel.dispatchEvent(ProductDescriptionUiEvent.OnVirtualTryOnTap)
                    }, modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(stringResource(R.string.view_in_your_space))
                }

                Column(
                    modifier =
                    Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(product.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(product.color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(product.description, fontSize = 14.sp)
                    Spacer(modifier = Modifier.padding(bottom = 100.dp))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            ) {
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(Modifier.weight(2f)) {
                        Text("Amount", fontWeight = FontWeight.Light)
                        Text(product.priceInCents.toDollars(), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {

                            productViewModel.dispatchEvent(ProductDescriptionUiEvent.OnAddToCartTap)
                        }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(R.string.add_to_cart), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun Int.toDollars(): String {
    return "$" + BigDecimal(this).movePointLeft(2).toString()
}