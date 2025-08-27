package com.ionic.nextwalls.data

data class Wallpapers(
    val id: String = "",
    val categoryId: String = "",
    val createdAt: Long = 0L,
    val imageUrl: String = "",
    val storagePath: String? = null,
    val title: String = ""
)