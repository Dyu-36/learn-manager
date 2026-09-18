package com.learnmanager.desktop

import java.awt.AWTException
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

object DesktopTray {
    private const val BRAND_COLOR = 0x3562CE

    private var trayIcon: TrayIcon? = null

    fun initialize(onOpen: () -> Unit, onExit: () -> Unit) {
        if (!SystemTray.isSupported() || trayIcon != null) return

        val popup = PopupMenu().apply {
            add(MenuItem("Mở LearnManager").apply { addActionListener { onOpen() } })
            addSeparator()
            add(MenuItem("Thoát").apply { addActionListener { onExit() } })
        }

        val icon = TrayIcon(createIcon(16), "LearnManager", popup).apply {
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

    /** PNG bytes of the application icon so the window can wrap it in a Compose Painter. */
    fun windowIconPng(): ByteArray {
        val image = createIcon(64) as BufferedImage
        val output = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "png", output)
        return output.toByteArray()
    }

    private fun createIcon(size: Int): Image {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics() as Graphics2D
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val margin = (size / 16f + 1f).toInt()
            val arc = size / 3
            graphics.color = Color(BRAND_COLOR)
            graphics.fillRoundRect(margin, margin, size - 2 * margin, size - 2 * margin, arc, arc)

            val fontHeight = (size * 0.62f).toInt()
            graphics.font = Font(Font.SANS_SERIF, Font.BOLD, fontHeight)
            val metrics = graphics.fontMetrics
            val text = "L"
            val x = (size - metrics.stringWidth(text)) / 2f
            val y = (size - metrics.height) / 2f + metrics.ascent
            graphics.color = Color.WHITE
            graphics.drawString(text, x, y)
        } finally {
            graphics.dispose()
        }
        return image
    }
}
