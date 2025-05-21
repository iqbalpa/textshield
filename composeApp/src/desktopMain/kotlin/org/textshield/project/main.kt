package org.textshield.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "TextShield - SMS Spam Filter",
        state = rememberWindowState()
    ) {
        window.minimumSize = Dimension(400, 600)
        App()
    }
}