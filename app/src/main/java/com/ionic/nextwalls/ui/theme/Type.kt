package com.ionic.nextwalls.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ionic.nextwalls.R

// Load custom fonts from res/font (make sure to add them in /res/font folder)
val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_bold, FontWeight.Bold)
)

val Cinzel = FontFamily(
    Font(R.font.cinzel_semibold, FontWeight.SemiBold)
)

val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_medium, FontWeight.Medium)
)

val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal)
)

val AppTypography = Typography(

    // App Title (Logo, Splash, Toolbar title)
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),

    // Wallpaper Title
    headlineLarge = TextStyle(
        fontFamily = Cinzel,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),

    // Category Title
    titleMedium = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),

    // Category Description
    bodyMedium = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp
    )
)
