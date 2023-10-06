package com.pluu.plugin.module.sample

import com.android.sdklib.SdkVersionInfo
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.intellij.openapi.project.Project
import com.pluu.plugin.PluuBundle
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
            val basePackage = "com.pluu.sample"
            return ConfigureFeatureSampleModuleStep(
                NewFeatureSampleModuleModel(
                    project = project,
                    moduleParent = ":",
                    projectSyncInvoker = projectSyncInvoker
                ),
                SdkVersionInfo.LOWEST_ACTIVE_API, basePackage, name
            )
        }
    }
}