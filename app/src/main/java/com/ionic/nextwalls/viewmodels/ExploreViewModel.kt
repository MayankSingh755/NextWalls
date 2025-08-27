package com.ionic.nextwalls.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ionic.nextwalls.data.Wallpapers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class ExploreViewModel : ViewModel() {
    private val _wallpapers = MutableStateFlow<List<Wallpapers>>(emptyList())
    val wallpapers: StateFlow<List<Wallpapers>> = _wallpapers.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadFavorites()
        refreshWallpapers()
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                loadFavorites()
            } else {
                _favorites.value = emptySet()
            }
        }
    }

    fun refreshWallpapers() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                // Generate a random value for Firestore query
                val randomValue = Random.nextDouble()

                // Fetch wallpapers starting from random value
                val snapshot = firestore.collection("wallpapers")
                    .orderBy("randomKey")
                    .startAt(randomValue)
                    .limit(20)
                    .get()
                    .await()

                var wallpaperList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Wallpapers::class.java)?.copy(id = doc.id)
                }

                // If less than 20 items were returned, fetch more from the start
                if (wallpaperList.size < 20) {
                    val extraSnapshot = firestore.collection("wallpapers")
                        .orderBy("randomKey")
                        .limit((20 - wallpaperList.size).toLong())
                        .get()
                        .await()

                    val extraWallpapers = extraSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Wallpapers::class.java)?.copy(id = doc.id)
                    }

                    wallpaperList = wallpaperList + extraWallpapers
                }

                // Fallback: If still empty, shuffle everything as last resort
                if (wallpaperList.isEmpty()) {
                    val fallbackSnapshot = firestore.collection("wallpapers").get().await()
                    wallpaperList = fallbackSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Wallpapers::class.java)?.copy(id = doc.id)
                    }.shuffled()
                }

                _wallpapers.value = wallpaperList
            } catch (e: Exception) {
                e.printStackTrace()
                _wallpapers.value = emptyList()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadFavorites() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val userDoc = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val favoritesList = userDoc.get("favourites") as? List<*>
                    val favoriteWallpapers = favoritesList?.filterIsInstance<Map<String, Any>>()
                    val favoriteIds = favoriteWallpapers?.mapNotNull { it["id"] as? String }?.toSet() ?: emptySet()

                    _favorites.value = favoriteIds
                } catch (_: Exception) {
                    _favorites.value = emptySet()
                }
            }
        } else {
            _favorites.value = emptySet()
        }
    }

    fun toggleFavorite(wallpaper: Wallpapers) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val userDocRef = firestore.collection("users").document(currentUser.uid)
                val userDoc = userDocRef.get().await()

                val currentFavorites = userDoc.get("favourites") as? List<Map<String, Any>> ?: emptyList()
                val favoriteIds = currentFavorites.map { it["id"] as String }.toSet()

                val updatedFavorites =
                    if (favoriteIds.contains(wallpaper.id)) {
                        currentFavorites.filter { (it["id"] as String) != wallpaper.id }
                    } else {
                        val newFavorite = mapOf(
                            "id" to wallpaper.id,
                            "title" to wallpaper.title,
                            "url" to wallpaper.imageUrl,
                        )
                        currentFavorites + newFavorite
                    }

                userDocRef.set(
                    mapOf("favourites" to updatedFavorites),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()

                val updatedFavoriteIds = updatedFavorites.map { it["id"] as String }.toSet()
                _favorites.value = updatedFavoriteIds

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
