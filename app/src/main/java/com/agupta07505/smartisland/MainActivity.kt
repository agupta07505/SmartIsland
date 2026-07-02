package com.agupta07505.smartisland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.agupta07505.smartisland.ui.SmartIslandHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF111827),
                    secondary = androidx.compose.ui.graphics.Color(0xFF2563EB),
                    background = androidx.compose.ui.graphics.Color(0xFFF7F8FA),
                    surface = androidx.compose.ui.graphics.Color.White
                )
            ) {
                SmartIslandHomeScreen()
            }
        }
    }
}
