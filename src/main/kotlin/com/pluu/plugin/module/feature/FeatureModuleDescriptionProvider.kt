package com.pluu.plugin.module.feature

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.intellij.openapi.project.Project
import com.pluu.plugin.PluuBundle
import icons.PluuIcons
import javax.swing.Icon

class FeatureModuleDescriptionProvider : ModuleDescriptionProvider {
    override fun getDescriptions(project: Project): Collection<ModuleGalleryEntry> = listOf(
        FeatureModuleTemplateGalleryEntry(),
    )

    private class FeatureModuleTemplateGalleryEntry : ModuleGalleryEntry {
        override val icon: Icon = PluuIcons.Konata
        override val name: String = PluuBundle.message("pluu.module.new.feature.title")
        override val description: String = PluuBundle.message("pluu.module.new.feature.description")
        override fun toString(): String = name

        override fun createStep(
            project: Project,
            moduleParent: String,
            projectSyncInvoker: ProjectSyncInvoker
        ): SkippableWizardStep<*> {
            val model = NewFeatureModuleModel.fromExistingProject(
                project = project,
                projectSyncInvoker = projectSyncInvoker,
                moduleParent = moduleParent,
                isLibrary = true
            )
            return ConfigureFeatureModuleStep(model)
        }
    }
}