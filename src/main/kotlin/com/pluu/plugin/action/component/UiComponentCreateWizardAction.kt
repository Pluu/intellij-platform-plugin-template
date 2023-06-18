package com.pluu.plugin.action.component

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android-npw/src/com/android/tools/idea/npw/actions/NewAndroidComponentAction.kt?q=NewAndroidComponentAction
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.model.RenderTemplateModel
import com.android.tools.idea.npw.project.getModuleTemplates
import com.android.tools.idea.npw.project.getPackageForPath
import com.android.tools.idea.npw.template.ConfigureTemplateParametersStep
import com.android.tools.idea.npw.template.TemplateResolver
import com.android.tools.idea.wizard.model.ModelWizard
import com.android.tools.idea.wizard.template.WizardUiContext
import com.android.tools.idea.wizard.ui.SimpleStudioWizardLayout
import com.android.tools.idea.wizard.ui.StudioWizardDialogBuilder
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.utils.ModuleUtils
import icons.StudioIcons
import org.jetbrains.android.facet.AndroidFacet
import java.io.File

@JvmField
val CREATED_FILES = DataKey.create<MutableList<File>>("CreatedFiles")

abstract class UiComponentCreateWizardAction(
    text: String,
    private val wizardUiContext: AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext,
    private val templateName: String,
    private val dialogTitle: String,
    private val stepTitle: String
) : AnAction(text, null, StudioIcons.Shell.Filetree.ANDROID_FILE) {

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = ModuleUtils.isAndroidModulePlace(e.dataContext)
    }

    final override fun actionPerformed(e: AnActionEvent) {
        val module = PlatformCoreDataKeys.MODULE.getData(e.dataContext) ?: return
        val facet = AndroidFacet.getInstance(module) ?: return
        var targetDirectory = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)
        if (targetDirectory != null && !targetDirectory.isDirectory) {
            targetDirectory = targetDirectory.parent
            assert(targetDirectory != null)
        }
        if (AndroidModel.get(facet) == null || targetDirectory == null) {
            return
        }
        val commandName = e.presentation.text
        val createdFiles = e.dataContext.getData(CREATED_FILES)
        openNewUiWizard(
            facet,
            module.project,
            targetDirectory,
            createdFiles,
            commandName = commandName,
            dialogTitle = dialogTitle,
            stepTitle = stepTitle
        )
    }

    private fun openNewUiWizard(
        facet: AndroidFacet,
        project: Project,
        targetDirectory: VirtualFile,
        createdFiles: MutableList<File>?,
        commandName: String,
        dialogTitle: String,
        stepTitle: String
    ) {
        val moduleTemplates = facet.getModuleTemplates(targetDirectory)
            .filter {
                // Do not allow to create Android Components from the templates into source sets without a source root.
                // This will filter out androidTest and unit tests source sets.
                it.paths.getSrcDirectory(null) != null
            }
        assert(moduleTemplates.isNotEmpty())
        val initialPackageSuggestion = facet.getPackageForPath(moduleTemplates, targetDirectory)
        val templateModel = RenderTemplateModel.fromFacet(
            facet,
            initialPackageSuggestion,
            moduleTemplates[0],
            commandName,
            ProjectSyncInvoker.DefaultProjectSyncInvoker(),
            true,
            wizardUiContext
        )
        val newTemplate = TemplateResolver.getAllTemplates()
            .filter { WizardUiContext.MenuEntry in it.uiContexts }
            .find { it.name == templateName }

        templateModel.newTemplate = newTemplate!!

        val templateTypeStep =
            ConfigureTemplateParametersStep(templateModel, stepTitle, moduleTemplates)
        val modelWizard = ModelWizard.Builder()
            .addStep(templateTypeStep)
            .build()
        StudioWizardDialogBuilder(modelWizard, dialogTitle)
            .setProject(project)
            .build(SimpleStudioWizardLayout())
            .show()
        createdFiles?.addAll(templateModel.createdFiles)
    }
}