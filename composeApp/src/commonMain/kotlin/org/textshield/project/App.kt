package org.textshield.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.textshield.project.presentation.di.Dependencies
import org.textshield.project.presentation.ui.screens.InboxScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        // Use the InboxViewModel from Dependencies
        InboxScreen(viewModel = Dependencies.inboxViewModel)
    }
}