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

class FavoritesViewModel : ViewModel() {
    private val _favoriteWallpapers = MutableStateFlow<List<Wallpapers>>(emptyList())
    val favoriteWallpapers: StateFlow<List<Wallpapers>> = _favoriteWallpapers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun loadFavorites() {
        val currentUser = auth.currentUser ?: return

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val favoritesList = userDoc.get("favourites") as? List<*> ?: emptyList<Any>()
                val favoriteWallpapers =
                    favoritesList.filterIsInstance<Map<String, Any>>().map { favoriteMap ->
                        Wallpapers(
                            id = favoriteMap["id"] as? String ?: "",
                            title = favoriteMap["title"] as? String ?: "",
                            imageUrl = favoriteMap["url"] as? String ?: "",
                        )
                    }

                _favoriteWallpapers.value = favoriteWallpapers
            } catch (e: Exception) {
                // Handle error silently or log it
                _favoriteWallpapers.value = emptyList()
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeFavorite(wallpaper: Wallpapers) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val userDocRef = firestore.collection("users").document(currentUser.uid)
                val userDoc = userDocRef.get().await()

                // Get current favorites
                val currentFavorites =
                    userDoc.get("favourites") as? List<Map<String, Any>> ?: emptyList()

                // Remove the wallpaper from favorites
                val updatedFavorites =
                    currentFavorites.filter { (it["id"] as String) != wallpaper.id }

                // Update Firestore
                userDocRef.set(
                    mapOf("favourites" to updatedFavorites),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()

                // Update local state
                _favoriteWallpapers.value =
                    _favoriteWallpapers.value.filter { it.id != wallpaper.id }

            } catch (e: Exception) {
                // Handle error - could show a toast or log the error
                e.printStackTrace()
            }
        }
    }
}