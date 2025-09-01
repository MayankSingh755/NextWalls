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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ionic.nextwalls.R
import com.ionic.nextwalls.components.AuthState
import com.ionic.nextwalls.data.WallpaperMetadata
import com.ionic.nextwalls.viewmodels.AuthViewModel
import com.ionic.nextwalls.viewmodels.ExploreViewModel
import com.ionic.nextwalls.viewmodels.WallpaperViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ionic.nextwalls.data.Wallpapers

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dominantColor),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar with back and favorite buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Row {
                            // Favorite button
                            IconButton(onClick = {
                                if (authState is AuthState.Authenticated) {
                                    wallpapersViewModel.toggleFavorite(wallpaper!!)
                                    Toast.makeText(context,
                                        context.getString(R.string.favorite_updated), Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.please_sign_in_first),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }) {
                                Icon(
                                    imageVector = if (favorites.contains(wallpaper!!.id))
                                        Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (favorites.contains(wallpaper!!.id)) Color.Red else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Share button
                            IconButton(onClick = {
                                shareWallpaper(context, wallpaper!!)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wallpaper Image
                    SubcomposeAsyncImage(
                        model = wallpaper!!.imageUrl,
                        contentDescription = wallpaper!!.title,
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(280.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    // App logo and title row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.next_walls_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = wallpaper!!.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                    }

                    // Category description
                    val metadata by viewModel.wallpaperMetadata.collectAsState()
                    metadata?.category?.desc?.takeIf { it.isNotEmpty() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Metadata section
                    WallpaperMetadataSection(
                        metadata = metadata,
                        onReportClick = {
                            viewModel.reportWallpaper(context, wallpaper!!)
                        }
                    )

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // Save button
                        ActionButton(
                            painter = painterResource(R.drawable.rounded_download_2_24),
                            text = stringResource(R.string.save),
                            isLoading = isDownloading,
                            backgroundColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ) {
                            viewModel.downloadWallpaper(context, wallpaper!!)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Set button
                        ActionButton(
                            painter = painterResource(R.drawable.rounded_wallpaper_24),
                            text = stringResource(R.string.set),
                            isLoading = isSettingWallpaper,
                            backgroundColor = Color.White.copy(alpha = 0.9f),
                            contentColor = Color.Black
                        ) {
                            showSetWallpaperDialog = true
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
private fun WallpaperMetadataSection(
    metadata: WallpaperMetadata?,
    onReportClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 grid for metadata section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetaColumn(
                        icon = painterResource(R.drawable.outline_mobile_24),
                        label = stringResource(R.string.resolution),
                        value = metadata?.resolution?.toString() ?: stringResource(R.string.loading)
                    )
                    MetaColumn(
                        icon = painterResource(R.drawable.rounded_wallpaper_24),
                        label = stringResource(R.string.aspect_ratio),
                        value = metadata?.aspectRatio ?: stringResource(R.string.loading)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetaColumn(
                        icon = painterResource(R.drawable.next_walls_logo),
                        label = "Category",
                        value = metadata?.category?.name ?: stringResource(R.string.loading)
                    )
                    MetaColumn(
                        icon = painterResource(R.drawable.rounded_download_2_24),
                        label = stringResource(R.string.file_size),
                        value = metadata?.fileSizeEstimate ?: stringResource(R.string.loading)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.uploaded, metadata?.uploadedAt ?: stringResource(R.string.loading)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                TextButton(
                    onClick = onReportClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Report", tint = Color.Red)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.report), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun MetaColumn(icon: Painter, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.width(140.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}


@Composable
private fun ActionButton(
    painter: Painter,
    text: String,
    isLoading: Boolean,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .height(56.dp)
            .width(140.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun SetWallpaperDialog(onDismiss: () -> Unit, onSetWallpaper: (WallpaperTarget) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.set_wallpaper),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.choose_where_to_apply_this_wallpaper),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { onSetWallpaper(WallpaperTarget.HOME) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.home_screen))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onSetWallpaper(WallpaperTarget.LOCK) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.lock_screen))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onSetWallpaper(WallpaperTarget.BOTH) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painterResource(R.drawable.outline_mobile_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.both_screens))
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.cancel)
                    )
                }
            }
        }
    }
}

suspend fun getDominantColorFromUrl(context: Context, imageUrl: String): Color {
    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build()
            val result = (loader.execute(request) as SuccessResult).drawable
            val bitmap = (result as BitmapDrawable).bitmap
            val palette = Palette.from(bitmap).generate()
            Color(palette.getDominantColor(android.graphics.Color.DKGRAY))
        } catch (_: Exception) {
            Color(0xFF222222)
        }
    }
}

fun shareWallpaper(context: Context, wallpaper: Wallpapers) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                context.getString(R.string.check_out_this_amazing_wallpaper)
            )
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(
                    R.string.download_this_wallpaper_title,
                    wallpaper.imageUrl,
                    wallpaper.title
                )
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    } catch (_: Exception) {
        Toast.makeText(context,
            context.getString(R.string.failed_to_share_wallpaper), Toast.LENGTH_SHORT).show()
    }
}
