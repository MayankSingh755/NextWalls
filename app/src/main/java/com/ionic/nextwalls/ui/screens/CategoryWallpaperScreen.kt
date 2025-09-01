package com.ionic.nextwalls.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionic.nextwalls.R
import com.ionic.nextwalls.components.AuthState
import com.ionic.nextwalls.viewmodels.AuthViewModel
import com.ionic.nextwalls.viewmodels.CategoryWallpaperViewModel
import com.ionic.nextwalls.viewmodels.ExploreViewModel
import android.widget.Toast
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryWallpaperScreen(
    categoryId: String,
    categoryName: String,
    onWallpaperClick: (String) -> Unit = {},
    viewModel: CategoryWallpaperViewModel = viewModel(),
    wallpapersViewModel: ExploreViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val favorites by wallpapersViewModel.favorites.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(categoryId) {
        if (categoryId.isNotEmpty()) {
            viewModel.loadWallpapersForCategory(categoryId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.loading_wallpapers, categoryName))
                    }
                }
            }
            wallpapers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(
                                R.string.no_wallpapers_found_for_right_now_but_we_are_adding_more_wallpapers_stay_with_us_categoryWallpaperScreen,
                                categoryName
                            ),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.check_back_later_for_more_wallpapers),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            else -> {
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
                            onClick = {
                                val wallpaperId = wallpaper.id.trim()
                                if (wallpaperId.isNotEmpty() && wallpaperId.isNotBlank()) {
                                    onWallpaperClick(wallpaperId)
                                } else {
                                    val errorMsg = context.getString(
                                        R.string.error_wallpaper_id_is_missing_or_empty_id,
                                        wallpaper.id
                                    )
                                    println(errorMsg)
                                    Toast.makeText(
                                        context,
                                        errorMsg,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onFavoriteClick = {
                                when (authState) {
                                    is AuthState.Authenticated -> {
                                        wallpapersViewModel.toggleFavorite(wallpaper)
                                    }
                                    else -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.please_sign_in_first),
                                            Toast.LENGTH_SHORT
                                        ).show()
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