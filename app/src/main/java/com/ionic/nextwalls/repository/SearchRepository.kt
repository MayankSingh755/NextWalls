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
            val titleResultsSnapshot = wallpapersCollection
                .orderBy("title")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .await()

            val titleResults = titleResultsSnapshot.documents.mapNotNull { document ->
                document.toObject(Wallpapers::class.java)?.copy(id = document.id)
            }

            val allWallpapersSnapshot = wallpapersCollection
                .get()
                .await()

            val allWallpapers = allWallpapersSnapshot.documents.mapNotNull { document ->
                document.toObject(Wallpapers::class.java)?.copy(id = document.id)
            }

            val containsResults = allWallpapers.filter { wallpaper ->
                wallpaper.title.lowercase().contains(searchQuery) &&
                        !titleResults.any { it.id == wallpaper.id }
            }

            titleResults + containsResults
        } catch (e: Exception) {
            println("SearchRepository Error: ${e.message}")
            emptyList()
        }
    }
}