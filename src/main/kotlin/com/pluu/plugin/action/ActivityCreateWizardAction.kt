package com.pluu.plugin.action

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android-npw/src/com/android/tools/idea/npw/actions/NewAndroidComponentAction.kt?q=NewAndroidComponentAction
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.model.RenderTemplateModel.Companion.fromFacet
import com.android.tools.idea.npw.project.getModuleTemplates
import com.android.tools.idea.npw.project.getPackageForPath
import com.android.tools.idea.npw.template.ConfigureTemplateParametersStep
import com.android.tools.idea.npw.template.TemplateResolver
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.wizard.model.ModelWizard
import com.android.tools.idea.wizard.template.WizardUiContext
import com.android.tools.idea.wizard.ui.SimpleStudioWizardLayout
import com.android.tools.idea.wizard.ui.StudioWizardDialogBuilder
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.MENU_GALLERY
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.PluuPlugin
import icons.StudioIcons
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidBundle
import java.io.File

@JvmField
val CREATED_FILES = DataKey.create<MutableList<File>>("CreatedFiles")

class ActivityCreateWizardAction : AnAction("Create Activity") {
    init {
        templatePresentation.icon = StudioIcons.Shell.Filetree.ACTIVITY
    }

    override fun actionPerformed(e: AnActionEvent) {
        val module = PlatformCoreDataKeys.MODULE.getData(e.dataContext) ?: return
        val facet = AndroidFacet.getInstance(module) ?: return
        if (AndroidModel.get(facet) == null) {
            return
        }
        var targetDirectory = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)
        // If the user selected a simulated folder entry (eg "Manifests"), there will be no target directory
        if (targetDirectory != null && !targetDirectory.isDirectory) {
            targetDirectory = targetDirectory.parent
            assert(targetDirectory != null)
        }
        val activityDescription = e.presentation.text // e.g. "Empty Activity", "Tabbed Activity"
        val createdFiles = e.dataContext.getData(CREATED_FILES)
        openNewActivityWizard(
            facet,
            module.project,
            targetDirectory,
            createdFiles,
            activityDescription
        )
    }

    private fun openNewActivityWizard(
        facet: AndroidFacet,
        project: Project,
        targetDirectory: VirtualFile?,
        createdFiles: MutableList<File>?,
        activityDescription: String
    ) {
        val moduleTemplates = facet.getModuleTemplates(targetDirectory)
            .filter {
                // Do not allow to create Android Components from the templates into source sets without a source root.
                // This will filter out androidTest and unit tests source sets.
                it.paths.getSrcDirectory(null) != null
            }
        assert(moduleTemplates.isNotEmpty())
        val initialPackageSuggestion =
            if (targetDirectory == null) facet.getModuleSystem()
                .getPackageName() else facet.getPackageForPath(moduleTemplates, targetDirectory)
        val templateModel = fromFacet(
            facet, initialPackageSuggestion, moduleTemplates[0], "New $activityDescription",
            ProjectSyncInvoker.DefaultProjectSyncInvoker(),
            true, MENU_GALLERY
        )
        val newActivity = TemplateResolver.getAllTemplates()
            .filter { WizardUiContext.MenuEntry in it.uiContexts }
            .find { it.name == PluuPlugin.ACTIVITY_WITH_VIEWMODEL }

        templateModel.newTemplate = newActivity!!

        val dialogTitle = AndroidBundle.message(
            "android.wizard.new.activity.title"
        )
        val stepTitle = AndroidBundle.message(
            "android.wizard.config.activity.title"
        )

        val activityTypeStep = ConfigureTemplateParametersStep(templateModel, stepTitle, moduleTemplates)
        val modelWizard = ModelWizard.Builder()
            .addStep(activityTypeStep)
            .build()
        StudioWizardDialogBuilder(modelWizard, dialogTitle)
            .setProject(project)
            .build(SimpleStudioWizardLayout())
            .show()
        createdFiles?.addAll(templateModel.createdFiles)
    }
}