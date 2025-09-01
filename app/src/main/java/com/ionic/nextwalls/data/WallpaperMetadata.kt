package com.ionic.nextwalls.data

import com.ionic.nextwalls.components.ImageResolution

data class WallpaperMetadata(
    val resolution: ImageResolution? = null,
    val aspectRatio: String = "",
    val category: Category? = null,
    val uploadedAt: String = "",
    val fileSizeEstimate: String = ""
)

