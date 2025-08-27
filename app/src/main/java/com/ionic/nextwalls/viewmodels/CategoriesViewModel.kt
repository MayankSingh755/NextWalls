package com.ionic.nextwalls.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ionic.nextwalls.data.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CategoriesViewModel : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("categories")
                    .get()
                    .await()


                val categoryList = snapshot.documents.mapNotNull { doc ->

                    try {
                        val category = doc.toObject(Category::class.java)?.copy(id = doc.id)
                        category
                    } catch (_: Exception) {
                        null
                    }
                }
                _categories.value = categoryList

            } catch (_: Exception) {
            }
        }
    }
}