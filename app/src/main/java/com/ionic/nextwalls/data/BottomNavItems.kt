package com.ionic.nextwalls.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
            label = stringResource(R.string.categories),
            icon = painterResource(id = R.drawable.rounded_category_24),
            route = Screen.Categories.route
        ),
        BottomNavItems(
            label = stringResource(R.string.explore),
            icon = painterResource(id = R.drawable.outline_explore_24),
            route = Screen.Explore.route
        ),
        BottomNavItems(
            label = stringResource(R.string.favorite),
            icon = painterResource(id = R.drawable.rounded_favorite_24),
            route = Screen.Favorites.route
        ),
        BottomNavItems(
            label = stringResource(R.string.search),
            icon = painterResource(id = R.drawable.rounded_search_24),
            route = Screen.Search.route
        )
    )
}

