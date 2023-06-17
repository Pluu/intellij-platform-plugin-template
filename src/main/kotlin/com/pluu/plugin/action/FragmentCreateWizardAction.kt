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
import com.android.tools.idea.wizard.model.ModelWizard
import com.android.tools.idea.wizard.template.WizardUiContext
import com.android.tools.idea.wizard.ui.SimpleStudioWizardLayout
import com.android.tools.idea.wizard.ui.StudioWizardDialogBuilder
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.FRAGMENT_GALLERY
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.PluuPlugin
import icons.StudioIcons
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidBundle
import java.io.File

class FragmentCreateWizardAction : AnAction("Create Fragment") {
    init {
        templatePresentation.icon = StudioIcons.Shell.Filetree.ANDROID_FILE
    }

    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val module = PlatformCoreDataKeys.MODULE.getData(dataContext) ?: return
        val facet = AndroidFacet.getInstance(module) ?: return
        var targetDirectory = CommonDataKeys.VIRTUAL_FILE.getData(dataContext)
        // If the user selected a simulated folder entry (eg "Manifests"), there will be no target directory
        if (targetDirectory != null && !targetDirectory.isDirectory) {
            targetDirectory = targetDirectory.parent
            assert(targetDirectory != null)
        }
        if (AndroidModel.get(facet) == null || targetDirectory == null) {
            return
        }
        val fragmentDescription = e.presentation.text
        val createdFiles = dataContext.getData(CREATED_FILES)
        openNewFragmentWizard(
            facet,
            module.project,
            targetDirectory,
            createdFiles,
            "New $fragmentDescription"
        )
    }

    private fun openNewFragmentWizard(
        facet: AndroidFacet,
        project: Project,
        targetDirectory: VirtualFile,
        createdFiles: MutableList<File>?,
        commandName: String
    ) {
        val moduleTemplates = facet.getModuleTemplates(targetDirectory)
            .filter {
                // Do not allow to create Android Components from the templates into source sets without a source root.
                // This will filter out androidTest and unit tests source sets.
                it.paths.getSrcDirectory(null) != null
            }
        assert(moduleTemplates.isNotEmpty())
        val initialPackageSuggestion = facet.getPackageForPath(moduleTemplates, targetDirectory)
        val templateModel = fromFacet(
            facet, initialPackageSuggestion, moduleTemplates[0], commandName,
            ProjectSyncInvoker.DefaultProjectSyncInvoker(),
            true, FRAGMENT_GALLERY
        )
        val newFragment = TemplateResolver.getAllTemplates()
            .filter { WizardUiContext.MenuEntry in it.uiContexts }
            .find { it.name == PluuPlugin.FRAGMENT_WITH_VIEWMODEL }

        templateModel.newTemplate = newFragment!!

        val dialogTitle = AndroidBundle.message(
            "android.wizard.new.fragment.title"
        )
        val stepTitle = AndroidBundle.message(
            "android.wizard.config.fragment.title"
        )

        val fragmentTypeStep =
            ConfigureTemplateParametersStep(templateModel, stepTitle, moduleTemplates)
        val modelWizard = ModelWizard.Builder()
            .addStep(fragmentTypeStep)
            .build()
        StudioWizardDialogBuilder(modelWizard, dialogTitle)
            .setProject(project)
            .build(SimpleStudioWizardLayout())
            .show()
        createdFiles?.addAll(templateModel.createdFiles)
    }
}