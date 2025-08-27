package com.ionic.nextwalls.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ionic.nextwalls.data.Wallpapers
import kotlinx.coroutines.tasks.await

class SearchRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val wallpapersCollection = firestore.collection("wallpapers")

    suspend fun searchWallpapers(query: String): List<Wallpapers> {
        return try {
            val searchQuery = query.lowercase().trim()

            // Search by title (case-insensitive)
            val titleResults = wallpapersCollection
                .orderBy("title")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .await()
                .toObjects(Wallpapers::class.java)

            // Also search for titles containing the search term
            val allWallpapers = wallpapersCollection
                .get()
                .await()
                .toObjects(Wallpapers::class.java)

            val containsResults = allWallpapers.filter { wallpaper ->
                wallpaper.title.lowercase().contains(searchQuery) &&
                        !titleResults.any { it.id == wallpaper.id }
            }

            // Combine results, prioritizing exact matches
            titleResults + containsResults
        } catch (_: Exception) {
            emptyList()
        }
    }

//    suspend fun getAllWallpapers(): List<Wallpapers> {
//        return try {
//            wallpapersCollection
//                .orderBy("createdAt", Query.Direction.DESCENDING)
//                .get()
//                .await()
//                .toObjects(Wallpapers::class.java)
//        } catch (_: Exception) {
//            emptyList()
//        }
//    }
}