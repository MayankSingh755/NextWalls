package com.ionic.nextwalls.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.ionic.nextwalls.R
import com.ionic.nextwalls.data.Wallpapers
import com.ionic.nextwalls.components.extractDominantColor
import com.ionic.nextwalls.components.AuthState
import com.ionic.nextwalls.viewmodels.ExploreViewModel
import com.ionic.nextwalls.viewmodels.AuthViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    onWallpaperClick: (String) -> Unit = {},
    wallpapersViewModel: ExploreViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val wallpapers by wallpapersViewModel.wallpapers.collectAsState()
    val isRefreshing by wallpapersViewModel.isRefreshing.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val favorites by wallpapersViewModel.favorites.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { wallpapersViewModel.refreshWallpapers() }
        ) {
            if (wallpapers.isEmpty() && !isRefreshing) {
                // Empty state when no wallpapers
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No wallpapers found.")
                }
            } else if (wallpapers.isEmpty() && isRefreshing) {
                // Initial loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Show wallpapers in grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(wallpapers) { wallpaper ->
                        WallpapersList(
                            wallpaper = wallpaper,
                            isFavorite = favorites.contains(wallpaper.id),
                            onClick = { onWallpaperClick(wallpaper.id) },
                            onFavoriteClick = {
                                when (authState) {
                                    is AuthState.Authenticated -> {
                                        wallpapersViewModel.toggleFavorite(wallpaper)
                                    }
                                    else -> {
                                        Toast.makeText(context,
                                            context.getString(R.string.please_sign_in_first), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WallpapersList(
    wallpaper: Wallpapers,
    isFavorite: Boolean = false,
    onClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val dominantColor by produceState<Color>(initialValue = Color.Transparent, wallpaper.imageUrl) {
        value = if (wallpaper.imageUrl.isNotEmpty()) {
            extractDominantColor(wallpaper.imageUrl, context)
        } else {
            Color.Transparent
        }
    }

    val textColor = if (dominantColor.luminance() > 0.5f) Color.Black else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dominantColor != Color.Transparent) {
                dominantColor.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column {
            BoxWithConstraints {
                val imageHeight = maxWidth * 1.1f
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(wallpaper.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = wallpaper.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_broken_image_24),
                                contentDescription = "Error loading image",
                                tint = Color.White
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee()
                )

                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorite) {
                                R.drawable.baseline_favorite_24 // filled heart
                            } else {
                                R.drawable.rounded_favorite_24 // outlined heart
                            }
                        ),
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) {
                            Color.Red
                        } else if (dominantColor != Color.Transparent) {
                            dominantColor
                        } else {
                            Color.Gray
                        }
                    )
                }
            }
        }
    }
}