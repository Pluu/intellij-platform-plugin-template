package com.pluu.plugin

import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.LayoutBuilder
import java.awt.Component
import javax.swing.JLabel
import javax.swing.SwingConstants

/**
 * Creates a [JLabel], sets [JLabel.labelFor] and an optional [ContextHelpLabel].
 * It is recommended to create it inside of a cell if context help is used.
 */
internal fun Cell.labelFor(
    text: String,
    forComponent: Component,
    contextHelpText: String? = null
): JLabel {
    val label = if (contextHelpText == null) {
        JBLabel(text)
    } else {
        ContextHelpLabel.create(contextHelpText).apply {
            setText(text)
            horizontalTextPosition = SwingConstants.LEFT
        }
    }.apply {
        labelFor = forComponent
    }

    label()
    return label
}

internal fun LayoutBuilder.verticalGap() {
    row {
        label("")
    }
}