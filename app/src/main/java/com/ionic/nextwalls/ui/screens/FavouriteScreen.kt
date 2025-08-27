package com.ionic.nextwalls.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ionic.nextwalls.R
import com.ionic.nextwalls.data.Wallpapers
import com.ionic.nextwalls.ui.components.AuthState
import com.ionic.nextwalls.viewmodels.AuthViewModel
import com.ionic.nextwalls.viewmodels.FavoritesViewModel
import com.ionic.nextwalls.viewmodels.WallpapersViewModel

@Composable
fun FavoriteScreen(
    authViewModel: AuthViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel = viewModel(),
    wallpapersViewModel: WallpapersViewModel = viewModel(),
    onWallpaperClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val favoriteWallpapers by favoritesViewModel.favoriteWallpapers.collectAsState()
    val isLoading by favoritesViewModel.isLoading.collectAsState()
    var isSigningIn by remember { mutableStateOf(false) }

    // Load favorites when user is authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            favoritesViewModel.loadFavorites()
        }
    }

    // Launcher for Google Sign-In with detailed logging
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account?.idToken != null) {
                        authViewModel.signInWithGoogle(account.idToken)
                    } else {
                        Toast.makeText(context,
                            context.getString(R.string.failed_to_get_google_id_token), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            Activity.RESULT_CANCELED -> {
                Toast.makeText(context,
                    context.getString(R.string.sign_in_was_cancelled), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context,
                    context.getString(R.string.sign_in_failed_with_unexpected_result), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val webClientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        ""
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .requestProfile()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    when (authState) {
        is AuthState.Loading -> {
            LoadingScreen()
        }
        is AuthState.Unauthenticated -> {
            SignInScreen(
                isSigningIn = isSigningIn,
                onSignInClick = {
                    isSigningIn = true
                    googleSignInClient.signOut().addOnCompleteListener {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    }
                }
            )
        }
        is AuthState.Authenticated -> {
            AuthenticatedContent(
                user = (authState as AuthState.Authenticated).user,
                favoriteWallpapers = favoriteWallpapers,
                isLoading = isLoading,
                onSignOutClick = {
                    authViewModel.signOut()
                    googleSignInClient.signOut()
                },
                onRemoveFavorite = { wallpaper ->
                    favoritesViewModel.removeFavorite(wallpaper)
                    wallpapersViewModel.toggleFavorite(wallpaper)
                },
                onWallpaperClick = onWallpaperClick
            )
        }
        is AuthState.Error -> {
            ErrorScreen(
                message = (authState as AuthState.Error).message,
                onRetryClick = { authViewModel.checkAuthState() }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SignInScreen(
    isSigningIn: Boolean,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_user_not_signed_in),
            contentDescription = "Not Signed In",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.sign_in_to_save_favorites),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.access_your_favorite_wallpapers_across_devices),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSignInClick,
            enabled = !isSigningIn,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.continue_with_google),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.your_data_is_secure_and_never_shared),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun AuthenticatedContent(
    user: com.google.firebase.auth.FirebaseUser,
    favoriteWallpapers: List<Wallpapers>,
    isLoading: Boolean,
    onSignOutClick: () -> Unit,
    onRemoveFavorite: (Wallpapers) -> Unit,
    onWallpaperClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = stringResource(R.string.profile_picture),
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName ?: stringResource(R.string.user),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.favorites, favoriteWallpapers.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = "Sign Out", style = MaterialTheme.typography.bodySmall)
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            favoriteWallpapers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_favorites_yet),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.start_adding_wallpapers_to_your_favorites),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteWallpapers) { wallpaper ->
                        WallpapersList(
                            wallpaper = wallpaper,
                            isFavorite = true,
                            onClick = { onWallpaperClick(wallpaper.id) },
                            onFavoriteClick = { onRemoveFavorite(wallpaper) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.something_went_wrong),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetryClick) {
            Text(text = stringResource(R.string.retry))
        }
    }
}