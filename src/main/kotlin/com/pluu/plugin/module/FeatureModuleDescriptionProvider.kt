package com.pluu.plugin.module

import com.android.sdklib.SdkVersionInfo.LOWEST_ACTIVE_API
import com.android.tools.idea.npw.model.NewProjectModel.Companion.getSuggestedProjectPackage
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.android.tools.idea.wizard.template.Category
import com.android.tools.idea.wizard.template.FormFactor
import com.intellij.openapi.project.Project
import icons.StudioIcons
import org.jetbrains.android.util.AndroidBundle.message
import javax.swing.Icon

class FeatureModuleDescriptionProvider : ModuleDescriptionProvider {
    override fun getDescriptions(project: Project): Collection<ModuleGalleryEntry> = listOf(
        FeatureModuleTemplateGalleryEntry(),
    )

    private class FeatureModuleTemplateGalleryEntry : ModuleGalleryEntry {
        override val icon: Icon = StudioIcons.Wizards.Modules.ANDROID_LIBRARY
        override val name: String = "Pluu > Android Library"
        override val description: String = message("com.pluu.plugin.module.feature.description")
        override fun toString(): String = name

        override fun createStep(
            project: Project,
            moduleParent: String,
            projectSyncInvoker: ProjectSyncInvoker
        ): SkippableWizardStep<*> {
            val basePackage = getSuggestedProjectPackage()
            val model = NewFeatureModuleModel.fromExistingProject(
                project = project,
                moduleParent = moduleParent,
                projectSyncInvoker = projectSyncInvoker,
                formFactor = FormFactor.Mobile,
                category = Category.Activity,
                isLibrary = true
            )
            return ConfigureFeatureModuleStep(model, LOWEST_ACTIVE_API, basePackage, name)
        }
    }
}