package com.pluu.plugin.toolWindow.designsystem.qualifiers

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import org.jdesktop.swingx.prompt.PromptSupport
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea

private val COMPONENT_GAP = JBUI.scale(4)

class QualifierConfigurationPanel(
    private val viewModel: QualifierConfigurationViewModel
) : JPanel(BorderLayout(1, 0)) {

    private val designSystemTypeLabel = JBLabel("Design system Type:")

    private val designSystemTypeComboBox = ComboBox(viewModel.designSystemTypes)

    private val sampleCodeLabel = JBLabel("Sample code:")

    private val sampleCodeTextArea = JTextArea().apply {
        setLineWrap(true)
        setWrapStyleWord(true)
        PromptSupport.setPrompt("Input sample code", this)
    }

    init {
        designSystemTypeComboBox.renderer = SimpleListCellRenderer.create("", DesignSystemType::displayName)

        add(JPanel(FlowLayout(FlowLayout.LEFT, COMPONENT_GAP, 0)).apply {
            add(designSystemTypeLabel)
            add(designSystemTypeComboBox)
        }, BorderLayout.NORTH)

        val bottom = JPanel(BorderLayout(0, COMPONENT_GAP))
        add(bottom, BorderLayout.SOUTH)
        with(bottom) {
            border = JBUI.Borders.empty(COMPONENT_GAP)
            add(sampleCodeLabel, BorderLayout.NORTH)
            add(JBScrollPane(sampleCodeTextArea).apply {
                border = BorderFactory.createCompoundBorder(
                    RoundedLineBorder(UIUtil.getTreeSelectionBackground(true), COMPONENT_GAP, JBUI.scale(2)),
                    JBUI.Borders.empty(COMPONENT_GAP)
                )
                preferredSize = Dimension(sampleCodeTextArea.preferredSize.width, 100)
                minimumSize = preferredSize
                setViewportView(sampleCodeTextArea)
            }, BorderLayout.SOUTH)
        }
    }
}