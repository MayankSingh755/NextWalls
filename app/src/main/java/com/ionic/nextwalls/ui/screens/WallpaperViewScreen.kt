package com.ionic.nextwalls.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ionic.nextwalls.R
import com.ionic.nextwalls.ui.components.AuthState
import com.ionic.nextwalls.viewmodels.AuthViewModel
import com.ionic.nextwalls.viewmodels.WallpaperViewViewModel
import com.ionic.nextwalls.viewmodels.WallpapersViewModel

enum class WallpaperTarget {
    HOME, LOCK, BOTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperViewScreen(
    wallpaperId: String,
    onBackClick: () -> Unit,
    viewModel: WallpaperViewViewModel = viewModel(),
    wallpapersViewModel: WallpapersViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val wallpaper by viewModel.wallpaper.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val isSettingWallpaper by viewModel.isSettingWallpaper.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val favorites by wallpapersViewModel.favorites.collectAsState()

    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var showSetWallpaperDialog by remember { mutableStateOf(false) }

    // Permission launcher for storage permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            wallpaper?.let { viewModel.downloadWallpaper(context, it) }
        } else {
            Toast.makeText(context, "Storage permission required to download wallpaper", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(wallpaperId) {
        viewModel.loadWallpaper(wallpaperId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            wallpaper != null -> {
                // Wallpaper Image
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(wallpaper!!.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = wallpaper!!.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { showControls = !showControls })
                        },
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
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
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Failed to load wallpaper", color = Color.White)
                        }
                    }
                )

                // Top Controls
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = wallpaper!!.title,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                when (authState) {
                                    is AuthState.Authenticated -> wallpapersViewModel.toggleFavorite(wallpaper!!)
                                    else -> Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    painter = painterResource(
                                        id = if (favorites.contains(wallpaper!!.id)) R.drawable.baseline_favorite_24
                                        else R.drawable.rounded_favorite_24
                                    ),
                                    contentDescription = if (favorites.contains(wallpaper!!.id)) "Remove from favorites" else "Add to favorites",
                                    tint = if (favorites.contains(wallpaper!!.id)) Color.Red else Color.White
                                )
                            }

                            IconButton(onClick = {
                                shareWallpaper(context, wallpaper!!.imageUrl, wallpaper!!.title)
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    )
                }

                // Bottom Controls
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ActionButton(
                                painter = painterResource(R.drawable.rounded_download_2_24),
                                text = "Download",
                                isLoading = isDownloading
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    viewModel.downloadWallpaper(context, wallpaper!!)
                                } else {
                                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                        PackageManager.PERMISSION_GRANTED -> viewModel.downloadWallpaper(context, wallpaper!!)
                                        else -> permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            }

                            ActionButton(
                                painter = painterResource(R.drawable.rounded_wallpaper_24),
                                text = "Set Wallpaper",
                                isLoading = isSettingWallpaper
                            ) { showSetWallpaperDialog = true }
                        }
                    }
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(R.drawable.outline_error_24), contentDescription = "Error", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Wallpaper not found", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onBackClick) { Text("Go Back") }
                    }
                }
            }
        }
    }

    // Set wallpaper dialog
    if (showSetWallpaperDialog && wallpaper != null) {
        SetWallpaperDialog(
            onDismiss = { showSetWallpaperDialog = false },
            onSetWallpaper = { target ->
                viewModel.setAsWallpaper(context, wallpaper!!, target)
                showSetWallpaperDialog = false
            }
        )
    }
}



@Composable
private fun ActionButton(painter: Painter, text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.height(56.dp).width(140.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.9f), contentColor = Color.Black),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(painter = painter, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SetWallpaperDialog(onDismiss: () -> Unit, onSetWallpaper: (WallpaperTarget) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Set Wallpaper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Choose where to apply this wallpaper:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = { onSetWallpaper(WallpaperTarget.HOME) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Home Screen")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = { onSetWallpaper(WallpaperTarget.LOCK) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Lock Screen")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { onSetWallpaper(WallpaperTarget.BOTH) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(painterResource(R.drawable.outline_mobile_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Both Screens")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

private fun shareWallpaper(context: Context, imageUrl: String, title: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "Check out this amazing wallpaper: $imageUrl")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Wallpaper"))
}
