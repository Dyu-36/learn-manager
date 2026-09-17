package com.learnmanager.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.learnmanager.LearnManagerApp

fun main() = application {
    var visible by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        DesktopTray.initialize(
            onOpen = { visible = true },
            onExit = { exitApplication() },
        )
        onDispose { DesktopTray.dispose() }
    }

    Window(
        visible = visible,
        onCloseRequest = { visible = false },
        title = "LearnManager",
    ) {
        LearnManagerApp()
    }
}
