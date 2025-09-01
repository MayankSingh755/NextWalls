package com.ionic.nextwalls.viewmodels

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ionic.nextwalls.data.Wallpapers
import com.ionic.nextwalls.components.ImageResolution
import com.ionic.nextwalls.components.ImageResolutionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import com.ionic.nextwalls.R
import com.ionic.nextwalls.data.Category
import com.ionic.nextwalls.data.WallpaperMetadata

class WallpaperViewViewModel : ViewModel() {
    private val _wallpaper = MutableStateFlow<Wallpapers?>(null)
    val wallpaper: StateFlow<Wallpapers?> = _wallpaper.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isSettingWallpaper = MutableStateFlow(false)
    val isSettingWallpaper: StateFlow<Boolean> = _isSettingWallpaper.asStateFlow()

    private val _wallpaperMetadata = MutableStateFlow<WallpaperMetadata?>(null)
    val wallpaperMetadata: StateFlow<WallpaperMetadata?> = _wallpaperMetadata.asStateFlow()

    private val _isLoadingMetadata = MutableStateFlow(false)

    private val firestore = FirebaseFirestore.getInstance()

    fun loadWallpaper(wallpaperId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = firestore.collection("wallpapers")
                    .document(wallpaperId)
                    .get()
                    .await()

                val wallpaper = document.toObject(Wallpapers::class.java)?.copy(id = document.id)
                _wallpaper.value = wallpaper

                wallpaper?.let { loadWallpaperMetadata(it) }

            } catch (e: Exception) {
                e.printStackTrace()
                _wallpaper.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadWallpaperMetadata(wallpaper: Wallpapers) {
        viewModelScope.launch {
            _isLoadingMetadata.value = true
            try {
                // Load image resolution
                val resolution = ImageResolutionUtils.getResolutionFromUrl(wallpaper.imageUrl)

                // Load category data
                val category = loadCategory(wallpaper.categoryId)

                // Format uploaded date
                val uploadedAt = formatUploadedDate(wallpaper.createdAt)

                // Calculate aspect ratio
                val aspectRatio = resolution?.let {
                    val ratio = it.getAspectRatio()
                    when {
                        ratio > 1.7 -> "16:9"
                        ratio > 1.4 -> "3:2"
                        ratio > 1.2 -> "4:3"
                        ratio > 0.9 -> "1:1"
                        ratio > 0.7 -> "3:4"
                        ratio > 0.5 -> "2:3"
                        else -> "9:16"
                    }
                } ?: "Unknown"

                val fileSizeEstimate = resolution?.let {
                    val pixels = it.width * it.height
                    val estimatedBytes = (pixels * 3) / 4
                    ImageResolutionUtils.formatFileSize(estimatedBytes.toLong())
                } ?: "Unknown"

                _wallpaperMetadata.value = WallpaperMetadata(
                    resolution = resolution,
                    aspectRatio = aspectRatio,
                    category = category,
                    uploadedAt = uploadedAt,
                    fileSizeEstimate = fileSizeEstimate
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _wallpaperMetadata.value = null
            } finally {
                _isLoadingMetadata.value = false
            }
        }
    }

    private suspend fun loadCategory(categoryId: String): Category? {
        return try {
            val document = firestore.collection("categories")
                .document(categoryId)
                .get()
                .await()

            document.toObject(Category::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatUploadedDate(timestamp: Long): String {
        return try {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(date)
        } catch (_: Exception) {
            "Unknown"
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun reportWallpaper(context: Context, wallpaper: Wallpapers) {
        try {
            val email = "devmayank755@gmail.com"
            val subject = "Report Wallpaper: ${wallpaper.title}"
            val body = """
                I would like to report the following wallpaper:
                
                Wallpaper Title: ${wallpaper.title}
                Wallpaper ID: ${wallpaper.id}
                
                Reason for reporting:
                [Please describe the issue]
                
                Additional details:
                [Please provide any additional information]
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:".toUri()
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context,
                    context.getString(R.string.no_email_app_found), Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(context,
                context.getString(R.string.failed_to_open_email_app), Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadWallpaper(context: Context, wallpaper: Wallpapers) {
        viewModelScope.launch {
            _isDownloading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadImage(context, wallpaper.imageUrl, wallpaper.title)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (result) context.getString(R.string.wallpaper_downloaded_successfully) else context.getString(
                            R.string.download_failed
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun setAsWallpaper(context: Context, wallpaper: Wallpapers, setFor: com.ionic.nextwalls.ui.screens.WallpaperTarget) {
        viewModelScope.launch {
            _isSettingWallpaper.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    setWallpaper(context, wallpaper.imageUrl, setFor)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (result) context.getString(R.string.wallpaper_set_successfully) else context.getString(R.string.failed_to_set_wallpaper),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context,
                        context.getString(R.string.failed_to_set_wallpaper_, e.message), Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isSettingWallpaper.value = false
            }
        }
    }

    private fun downloadImage(context: Context, imageUrl: String, fileName: String): Boolean {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()

            val inputStream = connection.getInputStream()
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NextWalls")
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    val outputStream = context.contentResolver.openOutputStream(it)
                    outputStream?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    }
                }
                true
            } else {
                // For older versions
                val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "NextWalls")
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }

                val file = File(picturesDir, "$fileName.jpg")
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()

                // Notify gallery
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(file)
                context.sendBroadcast(intent)

                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun setWallpaper(context: Context, imageUrl: String, target: com.ionic.nextwalls.ui.screens.WallpaperTarget): Boolean {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()

            val inputStream = connection.getInputStream()
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            val wallpaperManager = WallpaperManager.getInstance(context)

            when (target) {
                com.ionic.nextwalls.ui.screens.WallpaperTarget.HOME -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
                com.ionic.nextwalls.ui.screens.WallpaperTarget.LOCK -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
                com.ionic.nextwalls.ui.screens.WallpaperTarget.BOTH -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}