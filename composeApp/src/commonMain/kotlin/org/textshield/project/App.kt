package org.textshield.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.textshield.project.presentation.ui.screens.SimpleMessageScreen
import org.textshield.project.presentation.ui.theme.TextShieldTheme

@Composable
@Preview
fun App() {
    TextShieldTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SimpleMessageScreen()
        }
    }
}

expect fun getPlatformName(): String