package com.learnmanager.desktop

import java.awt.AWTException
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

object DesktopTray {
    private var trayIcon: TrayIcon? = null

    fun initialize(onOpen: () -> Unit, onExit: () -> Unit) {
        if (!SystemTray.isSupported() || trayIcon != null) return

        val popup = PopupMenu().apply {
            add(MenuItem("Mở LearnManager").apply { addActionListener { onOpen() } })
            addSeparator()
            add(MenuItem("Thoát").apply { addActionListener { onExit() } })
        }

        val icon = TrayIcon(createIcon(), "LearnManager", popup).apply {
            isImageAutoSize = true
            addActionListener { onOpen() }
        }

        try {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
        } catch (_: AWTException) {
            trayIcon = null
        }
    }

    fun notify(title: String, message: String) {
        val icon = trayIcon
        if (icon != null) {
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        } else {
            println("[LearnManager] $title - $message")
        }
    }

    fun dispose() {
        trayIcon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
    }

    private fun createIcon(): Image {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.fillRoundRect(4, 4, 24, 24, 8, 8)
        } finally {
            graphics.dispose()
        }
        return image
    }
}
