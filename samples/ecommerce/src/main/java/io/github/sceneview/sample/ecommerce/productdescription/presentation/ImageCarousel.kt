package io.github.sceneview.sample.ecommerce.productdescription.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImageCarousel(images: List<String>) {
    val pageCount = images.size
    val pagerState = rememberPagerState()
    Box( modifier = Modifier.fillMaxHeight(0.3f) ) {
        HorizontalPager(
            state = pagerState,
            pageCount = pageCount
        ) { page ->
            // Use your favorite image loading library here!
            GlideImage(
                model = images[page],
                contentScale = ContentScale.FillWidth,
                contentDescription = "image_$page"
            )
        }
        Row(
            Modifier
                .height(30.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)

                )

            }
        }
    }
}
