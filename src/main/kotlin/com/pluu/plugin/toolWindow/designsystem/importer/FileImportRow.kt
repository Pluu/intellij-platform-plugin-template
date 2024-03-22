package com.pluu.plugin.toolWindow.designsystem.importer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/importer/FileImportRow.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.ui.resourcemanager.widget.ChessBoardPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemType
import org.jdesktop.swingx.prompt.PromptSupport
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent

private val PREVIEW_SIZE = JBUI.size(150)
private val COMPONENT_GAP = JBUI.scale(4)

class FileImportRow(
    val viewModel: FileImportRowViewModel,
    isAddMode: Boolean
) : JPanel(BorderLayout()), Disposable {
    val preview = JBLabel().apply {
        horizontalAlignment = JBLabel.CENTER
    }

    private val previewWrapper = ChessBoardPanel().apply {
        preferredSize = PREVIEW_SIZE
        maximumSize = PREVIEW_SIZE
        border = JBUI.Borders.customLine(JBColor.border(), 1)
        add(preview)
    }

    private val sampleCodeTextArea = JTextArea(viewModel.sampleCode).apply {
        setLineWrap(true)
        setWrapStyleWord(true)
        PromptSupport.setPrompt("Input sample code", this)

        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                viewModel.updateSampleCode(this@apply.text)
                ComponentValidator.getInstance(this@apply).ifPresent(ComponentValidator::revalidate)
            }
        })

        ComponentValidator(this@FileImportRow).withValidator { ->
            viewModel.validateText(this.text, this)
        }.installOn(this)
            .revalidate()
    }

    private val configurationPanel = panel {
        row("Design system Type:") {
            comboBox(
                viewModel.selectableDesignSystemTypes,
                getRenderer("Select a type", DesignSystemType::name)
            ).bindItem(viewModel::designSystemType)
                .whenItemSelectedFromUi {
                    viewModel.selectDesignSystemType(it)
                }
        }
        row {
            label("Sample code:")
                .applyToComponent { preferredWidth = 150 }
            comboBox(
                viewModel.selectableApplicableFile.toList(),
                getRenderer("Select a type", ApplicableFileType::name)
            ).label("Applicable file:")
                .bindItem(viewModel::applicableFileType)
                .whenItemSelectedFromUi {
                    viewModel.selectApplicableFile(it)
                }
        }
        row {
            cell(
                JBScrollPane(sampleCodeTextArea).apply {
                    border = BorderFactory.createCompoundBorder(
                        RoundedLineBorder(UIUtil.getTreeSelectionBackground(true), COMPONENT_GAP, JBUI.scale(2)),
                        JBUI.Borders.empty(COMPONENT_GAP)
                    )
                    preferredSize = Dimension(sampleCodeTextArea.preferredSize.width, 80)
                    minimumSize = preferredSize
                    setViewportView(sampleCodeTextArea)
                }
            ).align(Align.FILL)
        }
    }

    private val middlePane = panel {
        border = JBUI.Borders.empty(COMPONENT_GAP)
        row {
            panel {
                row {
                    label(viewModel.fileName)
                    label(viewModel.fileSize)
                }
            }
            link("Do not import") {
                removeButtonClicked()
            }.align(AlignX.RIGHT)
                .enabled(isAddMode)
        }
        row {
            cell(configurationPanel).align(Align.FILL)
        }
    }

    init {
        add(panel {
            row {
                cell(previewWrapper).align(AlignY.TOP)
                cell(middlePane).align(Align.FILL)
            }
        })
        border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
            JBUI.Borders.empty(0, 4, 2, 4)
        )
        viewModel.updateCallback = ::update
        update()
    }

    fun update() {
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

    private fun <T> getRenderer(placeholderValue: String?, textRenderer: ((T) -> String?)?) =
        object : ColoredListCellRenderer<T?>() {
            override fun customizeCellRenderer(
                list: JList<out T?>,
                value: T?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                when (value) {
                    null -> append(placeholderValue ?: "Select a value...", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
                    else -> append(textRenderer?.let { it(value) } ?: value.toString())
                }
            }
        }

    override fun dispose() {

    }
}