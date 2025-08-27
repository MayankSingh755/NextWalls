package com.ionic.nextwalls.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ionic.nextwalls.data.bottomNavigationItems
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun NextWallsBottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavigationItems().forEach { navigationItem ->
            val selected = currentRoute == navigationItem.route
            val fontSize by animateIntAsState(targetValue = if (selected) 12 else 10)

            NavigationBarItem(
                selected = selected,
                label = {
                    Text(
                        text = navigationItem.label,
                        fontSize = fontSize.sp
                    )
                },
                icon = {
                    Icon(
                        navigationItem.icon,
                        contentDescription = navigationItem.label,
                        modifier = if (currentRoute == navigationItem.route) {
                            Modifier.size(28.dp)
                        } else {
                            Modifier.size(24.dp)
                        }
                    )
                },
                onClick = {
                    navController.navigate(navigationItem.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
