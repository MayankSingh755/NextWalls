package com.ionic.nextwalls.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ionic.nextwalls.R
import com.ionic.nextwalls.viewmodels.SearchViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlin.math.absoluteValue
import androidx.compose.ui.util.lerp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    wallpapersViewModel: com.ionic.nextwalls.viewmodels.ExploreViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    authViewModel: com.ionic.nextwalls.viewmodels.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onWallpaperClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val authState by authViewModel.authState.collectAsState()
    val favorites by wallpapersViewModel.favorites.collectAsState()
    val allWallpapers by wallpapersViewModel.wallpapers.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search wallpapers...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.updateSearchQuery("") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.searchWallpapers(uiState.searchQuery)
                    keyboardController?.hide()
                }
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on search state
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.searchQuery.isEmpty() -> {
                // Show carousel and recent searches when no search query
                Column {
                    // Carousel Section
                    if (allWallpapers.isNotEmpty()) {
                        WallpaperCarousel(
                            wallpapers = allWallpapers.shuffled().take(10),
                            authState = authState,
                            favorites = favorites,
                            onFavoriteClick = { wallpaper ->
                                when (authState) {
                                    is com.ionic.nextwalls.ui.components.AuthState.Authenticated -> {
                                        wallpapersViewModel.toggleFavorite(wallpaper)
                                    }
                                    else -> {
                                        android.widget.Toast.makeText(context,
                                            context.getString(R.string.please_sign_in_first),
                                            android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onWallpaperClick = onWallpaperClick
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Recent searches
                    RecentSearchesSection(
                        recentSearches = uiState.recentSearches,
                        onSearchClick = { query ->
                            viewModel.updateSearchQuery(query)
                            viewModel.searchWallpapers(query)
                            keyboardController?.hide()
                        },
                        onClearAll = { viewModel.clearRecentSearches() },
                        onRemoveSearch = { viewModel.removeRecentSearch(it) }
                    )
                }
            }

            uiState.searchResults.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                // No results found
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No wallpapers found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                // Show search results
                SearchResultsGrid(
                    wallpapers = uiState.searchResults,
                    authState = authState,
                    favorites = favorites,
                    onWallpaperClick = onWallpaperClick,
                    onFavoriteClick = { wallpaper ->
                        when (authState) {
                            is com.ionic.nextwalls.ui.components.AuthState.Authenticated -> {
                                wallpapersViewModel.toggleFavorite(wallpaper)
                            }
                            else -> {
                                android.widget.Toast.makeText(context,
                                    context.getString(R.string.please_sign_in_first),
                                    android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onRemoveSearch: (String) -> Unit
) {
    if (recentSearches.isEmpty()) return

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onClearAll) {
                Text("Clear All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(recentSearches) { search ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    onClick = { onSearchClick(search) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.rounded_history_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = search,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        IconButton(
                            onClick = { onRemoveSearch(search) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperCarousel(
    wallpapers: List<com.ionic.nextwalls.data.Wallpapers>,
    authState: com.ionic.nextwalls.ui.components.AuthState,
    favorites: Set<String>,
    onFavoriteClick: (com.ionic.nextwalls.data.Wallpapers) -> Unit,
    onWallpaperClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { wallpapers.size })

    // Auto-scroll effect
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(4000) // 4 seconds delay
            val nextPage = (pagerState.currentPage + 1) % wallpapers.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        Text(
            text = "Try Something New",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            pageSpacing = 16.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            val wallpaper = wallpapers[page]

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = pagerState.getOffsetDistanceInPages(page).absoluteValue
                        lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        ).also { scale ->
                            scaleX = scale
                            scaleY = scale
                        }
                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    }
                    .clickable { onWallpaperClick(wallpaper.id) },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box {
                    AsyncImage(
                        model = wallpaper.imageUrl,
                        contentDescription = wallpaper.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )

                    // Content overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Favorite button at top right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Card(
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.9f)
                                )
                            ) {
                                IconButton(onClick = { onFavoriteClick(wallpaper) }) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (favorites.contains(wallpaper.id)) {
                                                R.drawable.baseline_favorite_24
                                            } else {
                                                R.drawable.rounded_favorite_24
                                            }
                                        ),
                                        contentDescription = if (favorites.contains(wallpaper.id))
                                            "Remove from favorites" else "Add to favorites",
                                        tint = if (favorites.contains(wallpaper.id)) Color.Red else Color.Gray
                                    )
                                }
                            }
                        }

                        // Title at bottom
                        Text(
                            text = wallpaper.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(wallpapers.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (isSelected) 20.dp else 8.dp,
                            height = 8.dp
                        )
                        .background(
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .animateContentSize()
                )
            }
        }
    }
}

@Composable
private fun SearchResultsGrid(
    wallpapers: List<com.ionic.nextwalls.data.Wallpapers>,
    authState: com.ionic.nextwalls.ui.components.AuthState,
    favorites: Set<String>,
    onWallpaperClick: (String) -> Unit,
    onFavoriteClick: (com.ionic.nextwalls.data.Wallpapers) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(wallpapers) { wallpaper ->
            WallpapersList(
                wallpaper = wallpaper,
                isFavorite = favorites.contains(wallpaper.id),
                onClick = { onWallpaperClick(wallpaper.id) },
                onFavoriteClick = { onFavoriteClick(wallpaper) }
            )
        }
    }
}