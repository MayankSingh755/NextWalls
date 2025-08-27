package com.ionic.nextwalls.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.ionic.nextwalls.R
import com.ionic.nextwalls.navigation.Screen

data class BottomNavItems(
    val label: String = "",
    val icon: Painter,
    val route: String = ""
)

@Composable
fun bottomNavigationItems(): List<BottomNavItems> {
    return listOf(
        BottomNavItems(
            label = "Home",
            icon = painterResource(id = R.drawable.rounded_home_24),
            route = Screen.Home.route
        ),
        BottomNavItems(
            label = "Categories",
            icon = painterResource(id = R.drawable.rounded_category_24),
            route = Screen.Categories.route
        ),
        BottomNavItems(
            label = "Favorites",
            icon = painterResource(id = R.drawable.rounded_favorite_24),
            route = Screen.Favorites.route
        ),
        BottomNavItems(
            label = "Search",
            icon = painterResource(id = R.drawable.rounded_search_24),
            route = Screen.Search.route
        )
    )
}

