package com.pluu.plugin.module.sample

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.intellij.openapi.project.Project
import com.pluu.plugin.PluuBundle
import com.pluu.plugin.module.feature.NewFeatureModuleModel
import icons.PluuIcons
import javax.swing.Icon

class FeatureSampleModuleDescriptionProvider : ModuleDescriptionProvider {
    override fun getDescriptions(project: Project): Collection<ModuleGalleryEntry> = listOf(
        FeatureSampleModuleTemplateGalleryEntry(),
    )

    private class FeatureSampleModuleTemplateGalleryEntry : ModuleGalleryEntry {
        override val icon: Icon = PluuIcons.Konata
        override val name: String = PluuBundle.message("pluu.module.new.feature.sample.title")
        override val description: String = PluuBundle.message("pluu.module.new.feature.sample.description")

        override fun toString(): String = name

        override fun createStep(
            project: Project,
            moduleParent: String,
            projectSyncInvoker: ProjectSyncInvoker
        ): SkippableWizardStep<*> {
            val model = NewFeatureModuleModel.fromExistingProject(
                project = project,
                projectSyncInvoker = projectSyncInvoker,
                moduleParent = ":sample",
                isLibrary = false,
                isNeedBaseModule = true
            )
            return ConfigureFeatureSampleModuleStep(model)
        }
    }
}