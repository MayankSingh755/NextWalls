package com.ionic.nextwalls.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionic.nextwalls.data.Wallpapers
import com.ionic.nextwalls.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = false,
    val searchResults: List<Wallpapers> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)

class SearchViewModel : ViewModel() {

    private val searchRepository = SearchRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val maxRecentSearches = 5

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    fun searchWallpapers(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val results = searchRepository.searchWallpapers(query)
                addToRecentSearches(query)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchResults = results,
                    error = null
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Search failed. Please try again."
                )
            }
        }
    }

    private fun addToRecentSearches(query: String) {
        val currentSearches = _uiState.value.recentSearches.toMutableList()

        // Remove if already exists
        currentSearches.remove(query)

        // Add to the beginning
        currentSearches.add(0, query)

        // Keep only the last 5 searches
        if (currentSearches.size > maxRecentSearches) {
            currentSearches.removeAt(currentSearches.size - 1)
        }

        _uiState.value = _uiState.value.copy(recentSearches = currentSearches)
    }

    fun clearRecentSearches() {
        _uiState.value = _uiState.value.copy(recentSearches = emptyList())
    }

    fun removeRecentSearch(query: String) {
        val updatedSearches = _uiState.value.recentSearches.toMutableList()
        updatedSearches.remove(query)
        _uiState.value = _uiState.value.copy(recentSearches = updatedSearches)
    }
}