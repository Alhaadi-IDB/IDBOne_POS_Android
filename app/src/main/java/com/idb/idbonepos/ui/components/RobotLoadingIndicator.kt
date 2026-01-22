package com.idb.idbonepos.ui.components

import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.idb.idbonepos.R

@Composable
fun RobotLoadingIndicator(
    isLoading: Boolean = true,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            // Show animated GIF for loading
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageResource(R.drawable.loading_robot)
                        
                        // Start animation if it's an AnimatedImageDrawable
                        val drawable = drawable
                        if (drawable is AnimatedImageDrawable) {
                            drawable.start()
                        }
                    }
                },
                modifier = Modifier.size(size),
                update = { imageView ->
                    if (imageView.drawable == null) {
                        imageView.setImageResource(R.drawable.loading_robot)
                    }
                    val drawable = imageView.drawable
                    if (drawable is AnimatedImageDrawable && !drawable.isRunning) {
                        drawable.start()
                    }
                }
            )
            DisposableEffect(Unit) {
                onDispose {
                    // Cleanup handled by AndroidView lifecycle
                }
            }
        } else {
            // Show static PNG for default/error state
            Image(
                painter = painterResource(id = R.drawable.idb_robot),
                contentDescription = "IDB Robot",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        }
    }
}
