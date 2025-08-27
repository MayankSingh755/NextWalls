package com.ionic.nextwalls.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ionic.nextwalls.R
import com.ionic.nextwalls.ui.theme.NextWallsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextWallTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.White)) {
                        append("Next")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF42A5F5))) {
                        append("Walls")
                    }
                },
                fontSize = 24.sp,
                fontFamily = FontFamily(Font(R.font.playfair_display_bold))
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview
@Composable
fun NextWallTopBarPreview() {
    NextWallsTheme {
        NextWallTopBar()
    }
}