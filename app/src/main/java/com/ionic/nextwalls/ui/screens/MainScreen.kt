package com.ionic.nextwalls.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ionic.nextwalls.navigation.AppNavGraph
import com.ionic.nextwalls.components.NextWallTopBar
import com.ionic.nextwalls.components.NextWallsBottomNavBar

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBars = currentRoute?.startsWith("wallpaperView") != true

    Scaffold(
        topBar = {
            if (showBars) {
                NextWallTopBar()
            }
        },
        bottomBar = {
            if (showBars) {
                NextWallsBottomNavBar(navController)
            }
        }
    ) { padding ->
        AppNavGraph(
            navController = navController,
            paddingValues = if (showBars) padding else PaddingValues()
        )
    }
}

