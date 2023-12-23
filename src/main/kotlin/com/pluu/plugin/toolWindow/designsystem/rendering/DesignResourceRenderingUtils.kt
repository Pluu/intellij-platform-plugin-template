package com.pluu.plugin.toolWindow.designsystem.rendering

import com.intellij.util.ui.ImageUtil
import java.awt.Color
import java.awt.image.BufferedImage

internal val EMPTY_IMAGE = createIcon(Color(0, 0, 0, 0))
internal val ERROR_IMAGE = createIcon(Color(10, 10, 10, 10))

internal fun createIcon(color: Color?): BufferedImage = ImageUtil.createImage(
    80, 80, BufferedImage.TYPE_INT_ARGB
).apply {
    with(createGraphics()) {
        this.color = color
        fillRect(0, 0, 80, 80)
        dispose()
    }
}