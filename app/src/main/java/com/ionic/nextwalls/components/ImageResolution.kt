package com.ionic.nextwalls.components

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class ImageResolution(
    val width: Int,
    val height: Int
) {
    override fun toString(): String = "${width} x ${height}"

    fun getAspectRatio(): Double = width.toDouble() / height.toDouble()
}

object ImageResolutionUtils {

    suspend fun getResolutionFromUrl(imageUrl: String): ImageResolution? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                if (options.outWidth > 0 && options.outHeight > 0) {
                    ImageResolution(options.outWidth, options.outHeight)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    @SuppressLint("DefaultLocale")
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}