package com.pluu.plugin.module.sample

import com.android.tools.idea.npw.model.ExistingProjectModelData
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleModel
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.OptionalProperty
import com.android.tools.idea.observable.core.OptionalValueProperty
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.Recipe
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.NEW_MODULE
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.pluu.plugin.PluuBundle
import com.pluu.plugin.module.feature.PROPERTIES_BYTECODE_LEVEL_KEY
import com.pluu.plugin.module.feature.properties
import com.pluu.plugin.wizard.module.recipes.feature.sample.generateFeatureSampleModule

class NewFeatureSampleModuleModel(
    project: Project,
    moduleParent: String,
    projectSyncInvoker: ProjectSyncInvoker
) : ModuleModel(
    name = "",
    commandName = PluuBundle.message("pluu.module.new.feature.sample.title"),
    isLibrary = false,
    projectModelData = ExistingProjectModelData(project, projectSyncInvoker),
    moduleParent = moduleParent + "sample",
    wizardContext = NEW_MODULE
) {
    val baseApplication = OptionalValueProperty<Module>()
    val bytecodeLevel: OptionalProperty<BytecodeLevel> = OptionalValueProperty(getInitialBytecodeLevel())
    val conventionPlugin: BoolValueProperty = BoolValueProperty(true)

    init {
        if (applicationName.isEmpty.get()) {
            val msg: String = when {
                isLibrary -> "My Library"
                else -> "My Application"
            }
            applicationName.set(msg)
        }
    }

    override val loggingEvent: AndroidStudioEvent.TemplateRenderer
        get() = AndroidStudioEvent.TemplateRenderer.ANDROID_MODULE

    override val renderer = object : ModuleTemplateRenderer() {
        override val recipe: Recipe
            get() = { td ->
                generateFeatureSampleModule(
                    moduleData = td as ModuleTemplateData,
                    appTitle = applicationName.get(),
                    useVersionCatalog = true,
                    useConventionPlugins = conventionPlugin.get()
                )
            }

        override fun init() {
            super.init()
            moduleTemplateDataBuilder.setBaseFeature(baseApplication.value)
        }
    }

    private fun getInitialBytecodeLevel(): BytecodeLevel {
        if (isLibrary) {
            val savedValue = properties.getValue(PROPERTIES_BYTECODE_LEVEL_KEY)
            return BytecodeLevel.values().firstOrNull { it.toString() == savedValue } ?: BytecodeLevel.default
        }
        return BytecodeLevel.default
    }
}