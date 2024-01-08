package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.ui.resourcemanager.widget.ChessBoardPanel
import com.android.tools.idea.ui.resourcemanager.widget.Separator
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import org.jdesktop.swingx.prompt.PromptSupport
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent

private val PREVIEW_SIZE = JBUI.size(100)
private val COMPONENT_GAP = JBUI.scale(4)

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
    private val fileSize = JBLabel(viewModel.fileSize)

    private val doNotImportButton = LinkLabel<Any?>("Do not import", null) { _, _ ->
        removeButtonClicked()
    }.apply {
        isFocusable = true
    }

    private val designSystemTypeLabel = JBLabel("Design system Type:")

    private val designSystemTypeComboBox = ComboBox(viewModel.designSystemTypes).apply {
        renderer = SimpleListCellRenderer.create("", DesignSystemType::displayName)

        addItemListener { itemEvent ->
            when (itemEvent.stateChange) {
                ItemEvent.SELECTED -> {
                    val designSystemType = itemEvent.item as DesignSystemType
                    viewModel.selectDesignSystemType(designSystemType)
                }
            }
        }
    }

    private val sampleCodeLabel = JBLabel("Sample code:")

    private val sampleCodeTextArea = JTextArea(
        """
<FrameLayout
  android:layout_width="wrap_content"
  android:layout_height="wrap_content" />
""".trimIndent()
    ).apply {
        setLineWrap(true)
        setWrapStyleWord(true)
        PromptSupport.setPrompt("Input sample code", this)

        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                viewModel.updateSampleCode(this@apply.text)
            }
        })
    }

    private val configurationPanel = JPanel(BorderLayout(1, 0)).apply {
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

    private val middlePane = JPanel(BorderLayout()).apply {
        add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(fileName)
            add(separator())
            add(fileSize)
        }, BorderLayout.WEST)
        add(doNotImportButton, BorderLayout.EAST)
        add(configurationPanel, BorderLayout.SOUTH)
    }

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
        fileSize.text = viewModel.fileSize
        repaint()
    }

    private fun removeButtonClicked() {
        parent.let {
            it.remove(this)
            it.revalidate()
            it.repaint()
        }
        viewModel.removeFile()
    }

    private fun separator() = Separator(8, 4)
}