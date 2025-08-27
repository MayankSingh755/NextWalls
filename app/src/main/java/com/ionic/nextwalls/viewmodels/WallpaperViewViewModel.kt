package com.ionic.nextwalls.viewmodels

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ionic.nextwalls.data.Wallpapers
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

class WallpaperViewViewModel : ViewModel() {
    private val _wallpaper = MutableStateFlow<Wallpapers?>(null)
    val wallpaper: StateFlow<Wallpapers?> = _wallpaper.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isSettingWallpaper = MutableStateFlow(false)
    val isSettingWallpaper: StateFlow<Boolean> = _isSettingWallpaper.asStateFlow()

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
            } catch (e: Exception) {
                e.printStackTrace()
                _wallpaper.value = null
            } finally {
                _isLoading.value = false
            }
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
                        if (result) "Wallpaper downloaded successfully!" else "Download failed",
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
                        if (result) "Wallpaper set successfully!" else "Failed to set wallpaper",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to set wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isSettingWallpaper.value = false
            }
        }
    }

    private suspend fun downloadImage(context: Context, imageUrl: String, fileName: String): Boolean {
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

    private suspend fun setWallpaper(context: Context, imageUrl: String, target: com.ionic.nextwalls.ui.screens.WallpaperTarget): Boolean {
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
