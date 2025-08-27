package com.ionic.nextwalls.ui.components
//
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.ionic.nextwalls.data.Wallpapers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.tasks.await
//
//object FavoriteSyncManager {
//    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
//    val favoriteIds: Flow<Set<String>> = _favoriteIds.asStateFlow()
//
//    private val auth = FirebaseAuth.getInstance()
//    private val firestore = FirebaseFirestore.getInstance()
//
//    init {
//        // Listen to auth state changes
//        auth.addAuthStateListener { firebaseAuth ->
//            if (firebaseAuth.currentUser != null) {
//                loadFavoriteIds()
//            } else {
//                _favoriteIds.value = emptySet()
//            }
//        }
//    }
//
//    private suspend fun loadFavoriteIds() {
//        val currentUser = auth.currentUser ?: return
//
//        try {
//            val userDoc = firestore.collection("users")
//                .document(currentUser.uid)
//                .get()
//                .await()
//
//            val favoritesList = userDoc.get("favourites") as? List<*> ?: emptyList<Any>()
//            val favoriteIds = favoritesList.filterIsInstance<Map<String, Any>>()
//                .mapNotNull { it["id"] as? String }
//                .toSet()
//
//            _favoriteIds.value = favoriteIds
//        } catch (e: Exception) {
//            _favoriteIds.value = emptySet()
//        }
//    }
//
//    suspend fun toggleFavorite(wallpaper: Wallpapers): Boolean {
//        val currentUser = auth.currentUser ?: return false
//
//        try {
//            val userDocRef = firestore.collection("users").document(currentUser.uid)
//            val userDoc = userDocRef.get().await()
//
//            // Get current favorites
//            val currentFavorites = userDoc.get("favourites") as? List<Map<String, Any>> ?: emptyList()
//            val favoriteIds = currentFavorites.map { it["id"] as String }.toSet()
//
//            val updatedFavorites = if (favoriteIds.contains(wallpaper.id)) {
//                // Remove from favorites
//                currentFavorites.filter { (it["id"] as String) != wallpaper.id }
//            } else {
//                // Add to favorites
//                val newFavorite = mapOf(
//                    "id" to wallpaper.id,
//                    "title" to wallpaper.title,
//                    "url" to wallpaper.imageUrl,
//                )
//                currentFavorites + newFavorite
//            }
//
//            // Update Firestore
//            userDocRef.set(
//                mapOf("favourites" to updatedFavorites),
//                com.google.firebase.firestore.SetOptions.merge()
//            ).await()
//
//            // Update local state
//            val updatedFavoriteIds = updatedFavorites.map { it["id"] as String }.toSet()
//            _favoriteIds.value = updatedFavoriteIds
//
//            return updatedFavoriteIds.contains(wallpaper.id)
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return false
//        }
//    }
//
//    fun isFavorite(wallpaperId: String): Boolean {
//        return _favoriteIds.value.contains(wallpaperId)
//    }
//}