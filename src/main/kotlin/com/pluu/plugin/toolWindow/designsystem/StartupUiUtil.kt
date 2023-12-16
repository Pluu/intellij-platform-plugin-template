package com.pluu.plugin.toolWindow.designsystem

import java.awt.Font
import javax.swing.UIManager

object StartupUiUtil {
    @JvmStatic
    val labelFont: Font
        get() = UIManager.getFont("Label.font")
}