package io.github.sceneview.sample.ecommerce

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.sceneview.sample.ecommerce.productdescription.presentation.ProductDescriptionScreen
import io.github.sceneview.sample.ecommerce.productdescription.presentation.ProductDescriptionViewModel
import io.github.sceneview.sample.ecommerce.ui.theme.SceneViewTheme
import io.github.sceneview.sample.ecommerce.viewinyourspace.presentation.ViewInYourSpaceScreen
import io.github.sceneview.sample.ecommerce.viewinyourspace.presentation.ViewInYourSpaceViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // Ideally these are injected through dependency injection
        val viewInYourSpaceViewModel by viewModels<ViewInYourSpaceViewModel>()
        val productViewModel by viewModels<ProductDescriptionViewModel>()

        super.onCreate(savedInstanceState)
        val productId = 1
        setContent {
            SceneViewTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            ProductDescriptionScreen(
                                productId = productId,
                                navController = navController,
                                productViewModel = productViewModel
                            )
                        }
                        composable(
                            "virtual_try_on/{productId}",
                            arguments = listOf(navArgument("productId") {
                                type = NavType.StringType
                            })
                        ) {
                            val productIdArg = it.arguments?.getString("productId")
                            ViewInYourSpaceScreen(productIdArg?.toInt() ?: 0, viewInYourSpaceViewModel)
                        }
                    }
                }
            }
        }
    }
}