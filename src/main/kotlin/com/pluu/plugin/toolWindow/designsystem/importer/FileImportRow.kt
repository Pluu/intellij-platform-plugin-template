package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.ui.resourcemanager.widget.ChessBoardPanel
import com.android.tools.idea.ui.resourcemanager.widget.Separator
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.qualifiers.QualifierConfigurationPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

private val PREVIEW_SIZE = JBUI.size(100)

class FileImportRow(val viewModel: FileImportRowViewModel) : JPanel(BorderLayout()) {
    val preview = JBLabel().apply {
        horizontalAlignment = JBLabel.CENTER
    }

    private val previewWrapper = ChessBoardPanel().apply {
        preferredSize = PREVIEW_SIZE
        maximumSize = PREVIEW_SIZE
        border = JBUI.Borders.customLine(JBColor.border(), 1)
        add(preview)
    }

    private val fileName = JBLabel(viewModel.fileName)
    private val folderConfiguration = JBLabel(viewModel.qualifiers)
    private val fileSize = JBLabel(viewModel.fileSize)
    private val fileDimension = JBLabel(viewModel.fileDimension)

    private val doNotImportButton = LinkLabel<Any?>("Do not import", null) { _, _ -> removeButtonClicked() }.apply {
        isFocusable = true
    }

    private fun removeButtonClicked() {
        parent.let {
            it.remove(this)
            it.revalidate()
            it.repaint()
        }
        viewModel.removeFile()
    }

    private val middlePane = JPanel(BorderLayout()).apply {
        add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(fileName)
            add(separator())
            add(fileSize)
        }, BorderLayout.WEST)
        add(doNotImportButton, BorderLayout.EAST)
        add(QualifierConfigurationPanel(viewModel.qualifierViewModel), BorderLayout.SOUTH)
    }

    private fun separator() = Separator(8, 4)

    init {
        add(JPanel().apply {
            add(previewWrapper)
        }, BorderLayout.WEST)
        add(middlePane)
        border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
            JBUI.Borders.empty(0, 4, 2, 4)
        )
        viewModel.updateCallback = ::update
        update()
    }

    fun update() {
        fileName.text = viewModel.fileName
        folderConfiguration.text = viewModel.qualifiers
        fileSize.text = viewModel.fileSize
        fileDimension.text = viewModel.fileDimension
        repaint()
    }
}