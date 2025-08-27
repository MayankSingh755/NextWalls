package com.ionic.nextwalls.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ionic.nextwalls.R
import com.ionic.nextwalls.ui.components.AuthState
import com.ionic.nextwalls.viewmodels.AuthViewModel
import com.ionic.nextwalls.viewmodels.WallpaperViewViewModel
import com.ionic.nextwalls.viewmodels.ExploreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class WallpaperTarget {
    HOME, LOCK, BOTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperViewScreen(
    wallpaperId: String,
    onBackClick: () -> Unit,
    viewModel: WallpaperViewViewModel = viewModel(),
    wallpapersViewModel: ExploreViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val wallpaper by viewModel.wallpaper.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val isSettingWallpaper by viewModel.isSettingWallpaper.collectAsState()
    val favorites by wallpapersViewModel.favorites.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF222222)) }
    var showSetWallpaperDialog by remember { mutableStateOf(false) }

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
                // Extract dominant color
                LaunchedEffect(wallpaper!!.imageUrl) {
                    dominantColor = getDominantColorFromUrl(context, wallpaper!!.imageUrl)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dominantColor)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        IconButton(onClick = {
                            if (authState is AuthState.Authenticated) {
                                wallpapersViewModel.toggleFavorite(wallpaper!!)
                            } else {
                                Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                painter = painterResource(
                                    id = if (favorites.contains(wallpaper!!.id)) R.drawable.baseline_favorite_24
                                    else R.drawable.rounded_favorite_24
                                ),
                                contentDescription = "Favorite",
                                tint = if (favorites.contains(wallpaper!!.id)) Color.Red else Color.White
                            )
                        }
                    }

                    // Wallpaper Image
                    SubcomposeAsyncImage(
                        model = wallpaper!!.imageUrl,
                        contentDescription = wallpaper!!.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.next_walls_logo),
                                contentDescription = "App Logo",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = wallpaper!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ActionButton(
                                painter = painterResource(R.drawable.rounded_download_2_24),
                                text = stringResource(R.string.download),
                                isLoading = isDownloading
                            ) {
                                viewModel.downloadWallpaper(context, wallpaper!!)
                            }
                            ActionButton(
                                painter = painterResource(R.drawable.rounded_wallpaper_24),
                                text = stringResource(R.string.set_wallpaper),
                                isLoading = isSettingWallpaper
                            ) {
                                showSetWallpaperDialog = true
                            }
                        }
                    }
                }
            }
        }
    }

    // Set Wallpaper Dialog
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
        modifier = Modifier
            .height(56.dp)
            .width(140.dp),
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                Text(stringResource(R.string.set_wallpaper), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.choose_where_to_apply_this_wallpaper), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = { onSetWallpaper(WallpaperTarget.HOME) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.home_screen))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = { onSetWallpaper(WallpaperTarget.LOCK) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.lock_screen))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { onSetWallpaper(WallpaperTarget.BOTH) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(painterResource(R.drawable.outline_mobile_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.both_screens))
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(
                    stringResource(R.string.cancel)
                ) }
            }
        }
    }
}

private fun shareWallpaper(context: Context, imageUrl: String, title: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT,
            context.getString(R.string.check_out_this_amazing_wallpaper, imageUrl))
    }
    context.startActivity(Intent.createChooser(shareIntent,
        context.getString(R.string.share_wallpaper)))
}

suspend fun getDominantColorFromUrl(context: Context, imageUrl: String): Color {
    return withContext(Dispatchers.IO) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build()
        val result = (loader.execute(request) as SuccessResult).drawable
        val bitmap = (result as BitmapDrawable).bitmap
        val palette = Palette.from(bitmap).generate()
        Color(palette.getDominantColor(android.graphics.Color.DKGRAY))
    }
}