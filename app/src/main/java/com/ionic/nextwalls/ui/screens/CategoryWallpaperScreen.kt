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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionic.nextwalls.viewmodels.CategoryWallpaperViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.ionic.nextwalls.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryWallpaperScreen(
    categoryId: String,
    categoryName: String,
    onWallpaperClick: (String) -> Unit = {},
    viewModel: CategoryWallpaperViewModel = viewModel()
) {
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(categoryId) {
        viewModel.loadWallpapersForCategory(categoryId)
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
                        Text("Loading $categoryName wallpapers...")
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
                            stringResource(
                                R.string.no_wallpapers_found_for_right_now_but_we_are_adding_more_wallpapers_stay_with_us,
                                categoryName
                            )
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wallpapers) { wallpaper ->
                        WallpapersList(
                            wallpaper = wallpaper,
                            onClick = { onWallpaperClick(wallpaper.id) }
                        )
                    }
                }
            }
        }
    }
}