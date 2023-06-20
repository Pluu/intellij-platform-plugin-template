package com.pluu.plugin.module

import com.android.tools.idea.npw.module.ConfigureModuleStep
import com.android.tools.idea.npw.template.components.BytecodeLevelComboProvider
import com.android.tools.idea.npw.toWizardFormFactor
import com.android.tools.idea.npw.validator.ProjectNameValidator
import com.android.tools.idea.observable.ui.SelectedItemProperty
import com.android.tools.idea.observable.ui.TextProperty
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI.Borders.empty
import com.pluu.plugin.labelFor
import org.jetbrains.android.util.AndroidBundle.message
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

    override fun createMainPanel(): DialogPanel = panel {
        row {
            labelFor("Module name", moduleName, message("android.wizard.module.help.name"))
            moduleName(pushX)
        }

        row {
            labelFor("Package name", packageName)
            packageName(pushX)
        }

        row {
            labelFor("Language", languageCombo)
            languageCombo(growX)
        }

        row {
            labelFor("Minimum SDK", apiLevelCombo)
            apiLevelCombo(growX)
        }
    }.withBorder(empty(6))

    init {
        bindings.bindTwoWay(TextProperty(appName), model.applicationName)
        bindings.bindTwoWay(SelectedItemProperty(bytecodeCombo), model.bytecodeLevel)
        validatorPanel.registerValidator(model.applicationName, ProjectNameValidator())
    }

    override fun getPreferredFocusComponent() = if (appName.isVisible) appName else moduleName
}