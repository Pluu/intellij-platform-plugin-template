@file:Suppress("UseJBColor")

package com.pluu.plugin.toolWindow.designsystem.rendering

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/rendering/ResourceRenderingUtils.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.adtui.common.AdtUiUtils
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.JBUI
import icons.StudioIcons
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.image.BufferedImage
import javax.swing.SwingConstants

internal val EMPTY_IMAGE = createIcon(Color(0, 0, 0, 0))
internal val ERROR_IMAGE = createIcon(Color(10, 10, 10, 10))

internal fun createIcon(color: Color): BufferedImage = ImageUtil.createImage(
    80, 80, BufferedImage.TYPE_INT_ARGB
).apply {
    with(createGraphics()) {
        this.color = color
        fillRect(0, 0, 80, 80)
        dispose()
    }
}

internal fun createFailedIcon(dimension: Dimension): BufferedImage {
    @Suppress("UndesirableClassUsage") // Dimensions for BufferedImage are pre-scaled.
    val image = BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_ARGB)
    val label = JBLabel("Failed preview", StudioIcons.Common.WARNING, SwingConstants.CENTER).apply {
        verticalTextPosition = SwingConstants.BOTTOM
        horizontalTextPosition = SwingConstants.CENTER
        foreground = AdtUiUtils.DEFAULT_FONT_COLOR
        bounds = Rectangle(0, 0, dimension.width, dimension.height)
        validate()
    }
    image.createGraphics().let { g ->
        val labelFont = JBUI.Fonts.label(10f)
        val stringWidth = labelFont.getStringBounds(label.text, g.fontRenderContext).width
        val targetWidth = dimension.width - JBUI.scale(4) // Add some minor padding
        val scale = minOf(targetWidth.toFloat() / stringWidth.toFloat(), 1f) // Only scale down to fit.
        label.font = labelFont.deriveFont(scale * labelFont.size)
        label.paint(g)
        g.dispose()
    }
    return image
}

internal fun createDrawablePlaceholderImage(width: Int, height: Int): BufferedImage {
    return createImageAndPaint(width, height) {
        paintDrawablePlaceholderImage(it, width, height)
    }
}

private fun paintDrawablePlaceholderImage(g: Graphics2D, width: Int, height: Int) {
    val label = JBLabel(StudioIcons.Shell.ToolWindows.VISUAL_ASSETS, SwingConstants.CENTER).apply {
        bounds = Rectangle(0, 0, width, height)
        validate()
    }
    label.paint(g)
}

private fun createImageAndPaint(width: Int, height: Int, doPaint: (Graphics2D) -> Unit): BufferedImage {
    return ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        try {
            doPaint(g)
        } finally {
            g.dispose()
        }
    }
}