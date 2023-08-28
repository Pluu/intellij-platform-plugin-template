package com.pluu.plugin.module

import com.android.tools.idea.npw.module.ConfigureModuleStep
import com.android.tools.idea.npw.template.components.BytecodeLevelComboProvider
import com.android.tools.idea.npw.toWizardFormFactor
import com.android.tools.idea.npw.validator.ProjectNameValidator
import com.android.tools.idea.observable.ui.SelectedItemProperty
import com.android.tools.idea.observable.ui.SelectedProperty
import com.android.tools.idea.observable.ui.TextProperty
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI.Borders.empty
import com.pluu.plugin.utils.contextLabel
import org.jetbrains.android.util.AndroidBundle
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JTextField

class ConfigureFeatureModuleStep(
    model: NewFeatureModuleModel,
    minSdkLevel: Int,
    basePackage: String?,
    title: String
) : ConfigureModuleStep<NewFeatureModuleModel>(
    model = model,
    formFactor = model.formFactor.get().toWizardFormFactor(),
    minSdkLevel = minSdkLevel,
    basePackage = basePackage,
    title = title
) {
    private val appName: JTextField = JBTextField(model.applicationName.get())
    private val bytecodeCombo: JComboBox<BytecodeLevel> = BytecodeLevelComboProvider().createComponent()
    private val conventionPluginCheckbox: JCheckBox = JBCheckBox("Use Gradle convention plugin")

    override fun createMainPanel(): DialogPanel = panel {
        row(contextLabel("Module name", AndroidBundle.message("android.wizard.module.help.name"))) {
            cell(moduleName).align(AlignX.FILL)
        }

        row("Package name") {
            cell(packageName).align(AlignX.FILL)
        }

        row("Language") {
            cell(languageCombo).align(AlignX.FILL)
        }

        row("Minimum SDK") {
            cell(apiLevelCombo).align(AlignX.FILL)
        }

        row {
            cell(conventionPluginCheckbox).align(AlignX.FILL)
        }
    }.withBorder(empty(6))

    init {
        bindings.bindTwoWay(TextProperty(appName), model.applicationName)
        bindings.bindTwoWay(SelectedItemProperty(bytecodeCombo), model.bytecodeLevel)
        bindings.bindTwoWay(SelectedProperty(conventionPluginCheckbox), model.conventionPlugin)
        validatorPanel.registerValidator(model.applicationName, ProjectNameValidator())
    }

    override fun getPreferredFocusComponent() = if (appName.isVisible) appName else moduleName
}