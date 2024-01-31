package com.pluu.plugin.settings

import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionListModel
import com.intellij.ui.CommonActionsPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.utils.DesignSystemTypeNameValidator
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

class ConfigComponent(
    values: List<DesignSystemType>
) {
    val root: JPanel

    private val designSystemStatus = JBCheckBox("Enable design system")

    var isEnableDesignSystem: Boolean
        get() = designSystemStatus.isSelected
        set(value) {
            designSystemStatus.isSelected = value
        }

    private val myTopPanel: JPanel

    private val myActionsModel: CollectionListModel<String> = CollectionListModel()

    init {
        myActionsModel.add(values.map { it.displayName })
        val list = JBList(myActionsModel)
        list.visibleRowCount = 5

        myTopPanel = panel {
            row {
                cell(
                    ToolbarDecorator.createDecorator(list)
                        .setAddAction {
                            val dialog = InputComponentDialog(values)
                            if (dialog.showAndGet()) {
                                val text = dialog.newComponentName()
                                myActionsModel.add(text)
                                val selectedIndex = list.lastVisibleIndex
                                list.selectedIndex = selectedIndex
                                list.ensureIndexIsVisible(selectedIndex)
                            }
                        }
                        .setButtonComparator(
                            CommonActionsPanel.Buttons.ADD.text,
                            CommonActionsPanel.Buttons.REMOVE.text,
                            CommonActionsPanel.Buttons.UP.text,
                            CommonActionsPanel.Buttons.DOWN.text
                        ).createPanel()
                ).align(AlignX.FILL)
                    .enabledIf(designSystemStatus.selected)
            }.topGap(TopGap.SMALL)
        }

        root = panel {
            group("Design System") {
                row { cell(designSystemStatus) }
                row { cell(myTopPanel).align(AlignX.FILL) }
            }
        }
    }

    fun designSystemTypes(): List<String> = myActionsModel.toList()

    private class InputComponentDialog(
        values: List<DesignSystemType>
    ) : DialogWrapper(true) {

        private val names = values.map { it.displayName.lowercase() }

        private lateinit var myField: JTextField

        init {
            title = "New Design Component Type"
            okAction.isEnabled = false
            init()
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return myField
        }

        override fun createCenterPanel(): JComponent? = null

        override fun createNorthPanel(): JComponent {
            return panel {
                row {
                    myField = textField()
                        .label("Design System name:", LabelPosition.TOP)
                        .comment("Available \"a-zA-z0-9_\"")
                        .gap(RightGap.SMALL)
                        .applyToComponent {
                            (document as AbstractDocument).documentFilter = DesignSystemNameFilter()

                            val cv = ComponentValidator(this@InputComponentDialog.disposable)
                            document.addDocumentListener(object : DocumentAdapter() {
                                override fun textChanged(p: DocumentEvent) {
                                    val text = this@applyToComponent.text
                                    val errorText = DesignSystemTypeNameValidator.getErrorTextForFileResource(text)
                                    val validationInfo = if (errorText != null) {
                                        ValidationInfo(errorText, this@applyToComponent)
                                    } else if (names.contains(text.lowercase())) {
                                        ValidationInfo("Type already exists.", this@applyToComponent)
                                    } else {
                                        null
                                    }
                                    cv.updateInfo(validationInfo)
                                    okAction.isEnabled = validationInfo == null
                                }
                            })
                        }
                        .component
                }
            }
        }

        fun newComponentName(): String = myField.text
    }

    private class DesignSystemNameFilter : DocumentFilter() {
        override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
            for (c in text) {
                if (!DesignSystemTypeNameValidator.isValidCharacter(c.code)) {
                    return
                }
            }
            super.replace(fb, offset, length, text, attrs)
        }
    }
}