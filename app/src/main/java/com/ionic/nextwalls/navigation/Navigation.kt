package com.ionic.nextwalls.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ionic.nextwalls.ui.screens.CategoryScreen
import com.ionic.nextwalls.ui.screens.CategoryWallpaperScreen
import com.ionic.nextwalls.ui.screens.FavoriteScreen
import com.ionic.nextwalls.ui.screens.ExploreScreen
import com.ionic.nextwalls.ui.screens.SearchScreen
import com.ionic.nextwalls.ui.screens.WallpaperViewScreen

sealed class Screen(val route: String) {
    object Explore : Screen("home")
    object Categories : Screen("categories")
    object Favorites : Screen("favorites")
    object Search : Screen("search")
}

@Composable
fun AppNavGraph(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = Screen.Explore.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Explore.route) {
            ExploreScreen(
                onWallpaperClick = { wallpaperId ->
                    navController.navigate("wallpaperView/$wallpaperId")
                }
            )
        }
        composable(Screen.Categories.route) {
            CategoryScreen(onCategoryClick = { categoryId, categoryName ->
                navController.navigate("categoryWallpapers/$categoryId/$categoryName")
            })
        }
        composable(Screen.Favorites.route) {
            FavoriteScreen(
                onWallpaperClick = { wallpaperId ->
                    navController.navigate("wallpaperView/$wallpaperId")
                }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onWallpaperClick = { wallpaperId ->
                    navController.navigate("wallpaperView/$wallpaperId")
                }
            )
        }
        composable(
            route = "categoryWallpapers/{categoryId}/{categoryName}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryWallpaperScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                onWallpaperClick = { wallpaperId ->
                    navController.navigate("wallpaperView/$wallpaperId")
                }
            )
        }
        composable(
            route = "wallpaperView/{wallpaperId}",
            arguments = listOf(
                navArgument("wallpaperId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val wallpaperId = backStackEntry.arguments?.getString("wallpaperId") ?: ""
            WallpaperViewScreen(
                wallpaperId = wallpaperId,
                onBackClick = { navController.popBackStack() },

                )
        }
    }
}