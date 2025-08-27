package com.ionic.nextwalls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ionic.nextwalls.ui.screens.MainScreen
import com.ionic.nextwalls.ui.theme.NextWallsTheme

class MainActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextWallsTheme {
                MainScreen()
            }
        }
    }
}