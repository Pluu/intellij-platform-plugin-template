package com.pluu.plugin.module.sample

import com.android.AndroidProjectTypes
import com.android.sdklib.SdkVersionInfo
import com.android.tools.idea.npw.model.NewProjectModel.Companion.getSuggestedProjectPackage
import com.android.tools.idea.npw.module.ConfigureModuleStep
import com.android.tools.idea.npw.template.components.BytecodeLevelComboProvider
import com.android.tools.idea.npw.template.components.ModuleComboProvider
import com.android.tools.idea.npw.toWizardFormFactor
import com.android.tools.idea.npw.validator.ModuleSelectedValidator
import com.android.tools.idea.observable.core.OptionalProperty
import com.android.tools.idea.observable.ui.SelectedItemProperty
import com.android.tools.idea.observable.ui.SelectedProperty
import com.android.tools.idea.observable.ui.TextProperty
import com.android.tools.idea.project.AndroidProjectInfo
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI.Borders.empty
import com.pluu.plugin.PluuBundle
import com.pluu.plugin.utils.contextLabel
import org.jetbrains.android.util.AndroidBundle
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JTextField

class ConfigureFeatureSampleModuleStep(
    model: NewFeatureSampleModuleModel,
    basePackage: String? = getSuggestedProjectPackage(),
    title: String = PluuBundle.message("pluu.module.new.feature.sample.title")
) : ConfigureModuleStep<NewFeatureSampleModuleModel>(
    model = model,
    formFactor = model.formFactor.get().toWizardFormFactor(),
    minSdkLevel = SdkVersionInfo.LOWEST_ACTIVE_API,
    basePackage = basePackage,
    title = title
) {
    private val baseApplication: JComboBox<Module> = ModuleComboProvider().createComponent()

    private val appName: JTextField = JBTextField(model.applicationName.get())
    private val bytecodeCombo: JComboBox<BytecodeLevel> = BytecodeLevelComboProvider().createComponent()
    private val conventionPluginCheckbox: JCheckBox = JBCheckBox("Use Gradle convention plugin")

    override fun createMainPanel(): DialogPanel = panel {
        row("Base Module") {
            cell(baseApplication).align(AlignX.FILL)
        }

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
        AndroidProjectInfo.getInstance(model.project)
            .getAllModulesOfProjectType(AndroidProjectTypes.PROJECT_TYPE_LIBRARY)
            .forEach { module -> baseApplication.addItem(module) }
        val baseApplication: OptionalProperty<Module> = model.baseApplication
        bindings.bind(baseApplication, SelectedItemProperty(this.baseApplication))
        validatorPanel.registerValidator(baseApplication, ModuleSelectedValidator())

        listeners.listen(model.baseApplication) { value ->
            model.moduleName.set(
                buildList {
                    add("sample")
                    addAll(value.get().name.split(".").drop(1))
                }.joinToString(":")
            )
        }
        bindings.bindTwoWay(TextProperty(moduleName), model.moduleName)

        bindings.bindTwoWay(TextProperty(appName), model.applicationName)
        bindings.bindTwoWay(SelectedItemProperty(bytecodeCombo), model.bytecodeLevel)
        bindings.bindTwoWay(SelectedProperty(conventionPluginCheckbox), model.conventionPlugin)
    }

    override fun getPreferredFocusComponent() = if (appName.isVisible) appName else moduleName
}