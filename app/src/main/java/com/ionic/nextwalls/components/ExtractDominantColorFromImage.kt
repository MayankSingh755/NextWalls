package com.ionic.nextwalls.components

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun extractDominantColor(imageUrl: String, context: Context): Color {

    return withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                bitmap?.let {
                    val palette = Palette.from(it).generate()
                    val swatch = palette.vibrantSwatch
                        ?: palette.lightVibrantSwatch
                        ?: palette.darkVibrantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.lightMutedSwatch
                        ?: palette.darkMutedSwatch

                    swatch?.let { Color(it.rgb) } ?: Color.Transparent
                } ?: Color.Transparent
            } else {
                Color.Transparent
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Color.Transparent
        }
    }
}