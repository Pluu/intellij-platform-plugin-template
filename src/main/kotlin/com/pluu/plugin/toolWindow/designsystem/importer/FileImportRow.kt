package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.ui.resourcemanager.widget.ChessBoardPanel
import com.intellij.openapi.Disposable
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import org.jdesktop.swingx.prompt.PromptSupport
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel

private val PREVIEW_SIZE = JBUI.size(150)
private val COMPONENT_GAP = JBUI.scale(4)

class FileImportRow(
    val viewModel: FileImportRowViewModel
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

    private val configurationPanel = panel {
        row("Design system Type:") {
            comboBox(
                viewModel.selectableDesignSystemTypes.toList(),
                getRenderer("Select a type", DesignSystemType::displayName)
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
                getRenderer("Select applicable File", ApplicableFileType::name)
            ).label("Applicable file:")
                .bindItem(viewModel::applicableFileType)
                .whenItemSelectedFromUi {
                    viewModel.selectApplicableFile(it)
                }
        }
        row {
            textArea()
                .align(Align.FILL)
                .rows(4)
                .bindText(viewModel::sampleCode)
                .validationOnApply {
                    if (it.text.isNullOrEmpty()) {
                        error("Cannot be empty")
                    } else {
                        null
                    }
                }
                .applyToComponent {
                    border = BorderFactory.createCompoundBorder(
                        RoundedLineBorder(UIUtil.getTreeSelectionBackground(true), COMPONENT_GAP, JBUI.scale(2)),
                        JBUI.Borders.empty(COMPONENT_GAP)
                    )
                    setLineWrap(true)
                    setWrapStyleWord(true)
                    PromptSupport.setPrompt("Input sample code", this)
                }.whenTextChangedFromUi {
                    viewModel.updateSampleCode(it)
                }
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