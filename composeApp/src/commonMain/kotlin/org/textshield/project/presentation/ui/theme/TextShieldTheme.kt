package org.textshield.project.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
private val LightColors = lightColorScheme(
    primary = Color(0xFF2A93D5),         // Soft blue for primary actions
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9F5), 
    onPrimaryContainer = Color(0xFF0A3258),
    secondary = Color(0xFF4CAF50),        // Green for positive actions
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEFDC),
    onSecondaryContainer = Color(0xFF0C390E),
    tertiary = Color(0xFF9C27B0),         // Purple for selected items
    onTertiary = Color.White,
    errorContainer = Color(0xFFFFDAD6),  // Light red for spam message background
    onErrorContainer = Color(0xFF410002), // Dark red for spam message text
    error = Color(0xFFBF0025),           // Strong red for spam indicators
    background = Color(0xFFF8F9FA),      // Slightly off-white background
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),  // Light purple-ish
    onSurfaceVariant = Color(0xFF49454F)
)

// Dark theme colors
private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),         // Lighter blue for dark theme
    onPrimary = Color(0xFF0A3258),
    primaryContainer = Color(0xFF0C2744),
    onPrimaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFF81C784),        // Lighter green for dark theme
    onSecondary = Color(0xFF0C390E),
    secondaryContainer = Color(0xFF0C390E),
    onSecondaryContainer = Color(0xFFB7DFBA),
    tertiary = Color(0xFFCE93D8),         // Lighter purple for dark theme
    onTertiary = Color(0xFF3E1751),
    errorContainer = Color(0xFF930006),  // Darker red for spam message background
    onErrorContainer = Color(0xFFFFDAD6), // Light red for spam message text
    error = Color(0xFFFF6E6E),           // Light red for spam indicators
    background = Color(0xFF1C1B1F),      // Dark background
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF3A3A3A),  // Dark gray
    onSurfaceVariant = Color(0xFFCAC4D0)
)

@Composable
fun TextShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
} 