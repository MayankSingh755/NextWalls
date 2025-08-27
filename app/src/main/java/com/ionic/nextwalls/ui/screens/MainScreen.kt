package com.ionic.nextwalls.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.ionic.nextwalls.navigation.AppNavGraph
import com.ionic.nextwalls.ui.components.NextWallTopBar
import com.ionic.nextwalls.ui.components.NextWallsBottomNavBar

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        topBar = { NextWallTopBar() },
        bottomBar = { NextWallsBottomNavBar(navController) }
    ) { padding ->
        AppNavGraph(navController = navController, paddingValues = padding)
    }
}