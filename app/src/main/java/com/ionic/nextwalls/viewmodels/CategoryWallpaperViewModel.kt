package com.ionic.nextwalls.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ionic.nextwalls.data.Wallpapers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CategoryWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableStateFlow<List<Wallpapers>>(emptyList())
    val wallpapers: StateFlow<List<Wallpapers>> = _wallpapers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadWallpapersForCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("wallpapers")
                    .whereEqualTo("categoryId", categoryId)
                    .get()
                    .await()

                val wallpaperList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val wallpaper = doc.toObject(Wallpapers::class.java)
                        wallpaper
                    } catch (_: Exception) {
                        null
                    }
                }

                _wallpapers.value = wallpaperList

            } catch (_: Exception) {
                _wallpapers.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}