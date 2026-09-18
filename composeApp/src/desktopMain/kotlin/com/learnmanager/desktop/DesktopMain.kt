package com.learnmanager.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.learnmanager.LearnManagerApp
import java.awt.Dimension

fun main() = application {
    var visible by remember { mutableStateOf(true) }
    var notifiedHidden by remember { mutableStateOf(false) }
    val windowIcon = remember {
        BitmapPainter(org.jetbrains.skia.Image.makeFromEncoded(DesktopTray.windowIconPng()).toComposeImageBitmap())
    }

    DisposableEffect(Unit) {
        DesktopTray.initialize(
            onOpen = { visible = true },
            onExit = { exitApplication() },
        )
        onDispose { DesktopTray.dispose() }
    }

    Window(
        visible = visible,
        onCloseRequest = {
            visible = false
            if (!notifiedHidden) {
                notifiedHidden = true
                DesktopTray.notify(
                    title = "LearnManager vẫn chạy nền",
                    message = "Nhấn icon khay hệ thống để mở lại; chọn Thoát để tắt hẳn.",
                )
            }
        },
        title = "LearnManager",
        icon = windowIcon,
        state = rememberWindowState(width = 1080.dp, height = 720.dp),
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(680, 480)
            onDispose { }
        }
        LearnManagerApp()
    }
}
