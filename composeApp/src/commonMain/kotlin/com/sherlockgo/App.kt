// App.kt — sherlock-go entry point
package com.sherlockgo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import com.sherlockgo.ui.screens.GameScreen
import com.sherlockgo.model.GoGame

private val GoColors = darkColorScheme(
    background = androidx.compose.ui.graphics.Color(0xFFDEB887),
    surface = androidx.compose.ui.graphics.Color(0xFFD2B48C),
    primary = androidx.compose.ui.graphics.Color(0xFF5D4037)
)

@Composable
fun App() {
    MaterialTheme(colorScheme = GoColors) {
        GameScreen()
    }
}
