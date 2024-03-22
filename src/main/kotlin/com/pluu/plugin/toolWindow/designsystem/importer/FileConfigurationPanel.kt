package com.pluu.plugin.toolWindow.designsystem.importer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/qualifiers/QualifierConfigurationPanel.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import icons.StudioIcons
import java.awt.BorderLayout
import java.util.*
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent

private val ADD_BUTTON_BORDER = JBUI.Borders.empty(4, 0)
private val ADD_BUTTON_SIZE = JBUI.size(20)

private const val CLEAR_ALIAS_NAME = "Clear alias name"

class FileConfigurationPanel(
    val viewModel: FileConfigurationViewModel
) : JPanel(BorderLayout(0, 0)) {

    private val configurationChanged: (Observable, Any?) -> Unit = { _, _ ->
        viewModel.applyConfiguration()
        validateAddConfiguration()
    }

    private val qualifierContainer = JPanel(VerticalLayout(0, SwingConstants.LEFT))

    private val addQualifierButton = LinkLabel("Add another alias name", null, ::onAddQualifierLabelClicked)
        .also { label ->
            label.border = ADD_BUTTON_BORDER
            label.isEnabled = canAddConfigurationRow()
            label.isFocusable = true
        }

    private fun onAddQualifierLabelClicked(
        label: LinkLabel<Any?>,
        @Suppress("UNUSED_PARAMETER") ignored: Any?
    ) {
        addConfigurationRow()
        label.isEnabled = canAddConfigurationRow()
    }

    init {
        val initConfig = viewModel.getCurrentAliasName()
        if (initConfig.isNotEmpty()) {
            initConfig.forEach {
                val row = ConfigurationRow(viewModel, it)
                qualifierContainer.add(row)
            }
        }

        validateAddConfiguration()

        add(qualifierContainer)
        add(addQualifierButton, BorderLayout.SOUTH)
    }

    private fun validateAddConfiguration() {
        addQualifierButton.isEnabled = canAddConfigurationRow()
    }

    private fun addConfigurationRow() {
        val configurationRow = ConfigurationRow(viewModel)
        qualifierContainer.add(configurationRow)
        qualifierContainer.revalidate()
        qualifierContainer.repaint()
        configurationRow.assetNameTextField.requestFocus()
    }

    private fun canAddConfigurationRow(): Boolean =
//        viewModel.canAddQualifier()
//                && qualifierContainer.components
        qualifierContainer.components
            .filterIsInstance<ConfigurationRow>()
            .map { it.assetNameTextField }
            .all { it.text.isNotEmpty() }

    private inner class ConfigurationRow(
        private val viewModel: FileConfigurationViewModel,
        private val param: AliasConfigParam = viewModel.newConfigKey()
    ) : JPanel(HorizontalLayout(0, SwingConstants.CENTER)), Disposable {

        val assetNameTextField = JBTextField("", 30).apply {
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    param.paramValue = this@apply.text
                }
            })
        }

        private val deleteButton = createDeleteButton()

        init {
            add(JBLabel("Alias name:"))
            add(assetNameTextField)
            add(deleteButton)

            updateValuePanel(param)
        }

        private fun createDeleteButton(): ActionButton {
            val action = object : DumbAwareAction(CLEAR_ALIAS_NAME, CLEAR_ALIAS_NAME, StudioIcons.Common.CLOSE) {

                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

                override fun actionPerformed(e: AnActionEvent) {
                    viewModel.deleteAlias(param)
                    deleteRow()
                    validateAddConfiguration()
                }
            }
            return ActionButton(
                action,
                action.templatePresentation.clone(),
                "Resource Explorer",
                ADD_BUTTON_SIZE
            ).apply { isFocusable = true }
        }

        private fun deleteRow() {
            with(parent) {
                remove(this@ConfigurationRow)
                revalidate()
                repaint()
            }
        }

        private fun updateValuePanel(param: AliasConfigParam) {
            assetNameTextField.text = param.paramValue
            param.addObserver(configurationChanged)
            revalidate()
            repaint()
        }

        override fun dispose() {

        }
    }

}