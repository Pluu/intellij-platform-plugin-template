package com.pluu.plugin.module.feature

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android-npw/src/com/android/tools/idea/npw/model/NewAndroidModuleModel.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.npw.model.ExistingProjectModelData
import com.android.tools.idea.npw.model.ProjectModelData
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleModel
import com.android.tools.idea.npw.project.GradleAndroidModuleTemplate.createDefaultModuleTemplate
import com.android.tools.idea.observable.core.*
import com.android.tools.idea.wizard.template.*
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.NEW_MODULE
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.pluu.plugin.wizard.module.recipes.feature.generateFeatureModule
import com.pluu.plugin.wizard.module.recipes.feature.sample.generateFeatureSampleModule
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplateRenderer as RenderLoggingEvent

class NewFeatureModuleModel(
    name: String,
    projectModelData: ProjectModelData,
    moduleParent: String,
    commandName: String = "New Module",
    isLibrary: Boolean = false,
    isFeatureSample: Boolean = false,
) : ModuleModel(
    name = name,
    commandName = commandName,
    isLibrary = isLibrary,
    projectModelData = projectModelData,
    _template = createDefaultModuleTemplate(projectModelData.project, name),
    moduleParent = moduleParent,
    wizardContext = NEW_MODULE
) {
    override val formFactor: ObjectProperty<FormFactor> =
        ObjectValueProperty(FormFactor.Mobile)

    override val category: ObjectProperty<Category> =
        ObjectValueProperty(Category.Activity)

    override val loggingEvent: AndroidStudioEvent.TemplateRenderer
        get() = RenderLoggingEvent.ANDROID_MODULE

    override val renderer = object : ModuleTemplateRenderer() {
        override val recipe: Recipe
            get() = { td: TemplateData ->
                if (isFeatureSample) {
                    generateFeatureSampleModule(
                        moduleData = td as ModuleTemplateData,
                        appTitle = applicationName.get(),
                        useVersionCatalog = true,
                        useConventionPlugins = conventionPlugin.get()
                    )
                } else {
                    generateFeatureModule(
                        data = td as ModuleTemplateData,
                        bytecodeLevel = bytecodeLevel.value,
                        useVersionCatalog = true,
                        useConventionPlugins = conventionPlugin.get()
                    )
                }
            }

        override fun init() {
            super.init()
            if (isFeatureSample) {
                moduleTemplateDataBuilder.setBaseFeature(baseModule.value)
            }
        }
    }

    val baseModule = OptionalValueProperty<Module>()
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

    private fun getInitialBytecodeLevel(): BytecodeLevel {
        if (isLibrary) {
            val savedValue = properties.getValue(PROPERTIES_BYTECODE_LEVEL_KEY)
            return BytecodeLevel.values().firstOrNull { it.toString() == savedValue } ?: BytecodeLevel.default
        }
        return BytecodeLevel.default
    }

    companion object {
        fun fromExistingProject(
            project: Project,
            projectSyncInvoker: ProjectSyncInvoker,
            moduleParent: String,
            isLibrary: Boolean = false,
            isNeedBaseModule: Boolean = false
        ): NewFeatureModuleModel = NewFeatureModuleModel(
            name = "",
            projectModelData = ExistingProjectModelData(project, projectSyncInvoker),
            moduleParent = moduleParent,
            isLibrary = isLibrary,
            isFeatureSample = isNeedBaseModule,
        )
    }
}