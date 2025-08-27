package com.ionic.nextwalls.viewmodels

import android.util.Log
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
                Log.d("CategoriesViewModel", "Loading categories...")

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("categories")
                    .get()
                    .await()

                Log.d("CategoriesViewModel", "Found ${snapshot.documents.size} category documents")

                val categoryList = snapshot.documents.mapNotNull { doc ->
                    Log.d("CategoriesViewModel", "Processing document: ${doc.id}")
                    Log.d("CategoriesViewModel", "Document data: ${doc.data}")

                    try {
                        val category = doc.toObject(Category::class.java)?.copy(id = doc.id)
                        Log.d("CategoriesViewModel", "Mapped category: $category")
                        category
                    } catch (e: Exception) {
                        Log.e("CategoriesViewModel", "Error mapping category ${doc.id}: ${e.message}")
                        null
                    }
                }

                Log.d("CategoriesViewModel", "Successfully mapped ${categoryList.size} categories")
                _categories.value = categoryList

            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error loading categories: ${e.message}", e)
            }
        }
    }
}