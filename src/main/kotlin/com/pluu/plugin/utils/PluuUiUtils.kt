package com.pluu.plugin.utils

import com.intellij.ui.ContextHelpLabel
import javax.swing.JLabel
import javax.swing.SwingConstants

internal fun contextLabel(text: String, contextHelpText: String): JLabel {
    return ContextHelpLabel.create(contextHelpText).apply {
        setText(text)
        horizontalTextPosition = SwingConstants.LEFT
    }
}