package com.pluu.plugin.module.feature

import com.android.tools.idea.gradle.npw.project.GradleAndroidModuleTemplate.createSampleTemplate
import com.android.tools.idea.npw.model.ExistingProjectModelData
import com.android.tools.idea.npw.model.ProjectModelData
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleModel
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.ObjectProperty
import com.android.tools.idea.observable.core.ObjectValueProperty
import com.android.tools.idea.observable.core.OptionalProperty
import com.android.tools.idea.observable.core.OptionalValueProperty
import com.android.tools.idea.projectsystem.NamedModuleTemplate
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.android.tools.idea.wizard.template.Category
import com.android.tools.idea.wizard.template.FormFactor
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.Recipe
import com.android.tools.idea.wizard.template.TemplateData
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.NEW_MODULE
import com.intellij.openapi.project.Project
import com.pluu.plugin.wizard.module.recipes.buildlogic.generateFeatureModule
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplateRenderer as RenderLoggingEvent

class NewFeatureModuleModel(
    projectModelData: ProjectModelData,
    template: NamedModuleTemplate,
    moduleParent: String,
    override val formFactor: ObjectProperty<FormFactor>,
    override val category: ObjectProperty<Category>,
    commandName: String = "New Module",
    override val isLibrary: Boolean = false,
    wizardContext: WizardUiContext
) : ModuleModel(
    "feature",
    commandName,
    isLibrary,
    projectModelData,
    template,
    moduleParent,
    wizardContext
) {
    val bytecodeLevel: OptionalProperty<BytecodeLevel> = OptionalValueProperty(getInitialBytecodeLevel())
    val conventionPlugin: BoolValueProperty = BoolValueProperty(true)

    override val renderer = object : ModuleTemplateRenderer() {
        override val recipe: Recipe
            get() = { td: TemplateData ->
                generateFeatureModule(
                    data = td as ModuleTemplateData,
                    bytecodeLevel = bytecodeLevel.value,
                    useVersionCatalog = true,
                    useConventionPlugins = conventionPlugin.get()
                )
            }
    }

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
        get() = RenderLoggingEvent.ANDROID_MODULE

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
            moduleParent: String,
            projectSyncInvoker: ProjectSyncInvoker,
            formFactor: FormFactor,
            category: Category,
            isLibrary: Boolean = false
        ): NewFeatureModuleModel = NewFeatureModuleModel(
            projectModelData = ExistingProjectModelData(project, projectSyncInvoker),
            template = createSampleTemplate(),
            moduleParent = moduleParent,
            formFactor = ObjectValueProperty(formFactor),
            category = ObjectValueProperty(category),
            isLibrary = isLibrary,
            wizardContext = NEW_MODULE
        )
    }
}