package com.pluu.plugin.action

import com.android.sdklib.SdkVersionInfo
import com.android.tools.idea.npw.model.NewProjectModel.Companion.getSuggestedProjectPackage
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.wizard.model.ModelWizard
import com.android.tools.idea.wizard.template.Category
import com.android.tools.idea.wizard.template.FormFactor
import com.android.tools.idea.wizard.ui.StudioWizardDialogBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.ui.JBUI
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.module.ConfigureFeatureModuleStep
import com.pluu.plugin.module.NewFeatureModuleModel
import com.pluu.plugin.utils.ModuleUtils
import icons.StudioIcons
import org.jetbrains.android.util.AndroidBundle.message

class FeatureModuleCreateWizardAction : AnAction(
    PluuPlugin.PLUU_MODULE,
    null,
    StudioIcons.Shell.Filetree.ANDROID_MODULE
) {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = ModuleUtils.isRootPlace(e.dataContext)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val basePackage = getSuggestedProjectPackage()
        val model = NewFeatureModuleModel.fromExistingProject(
            project = project,
            moduleParent = ":",
            projectSyncInvoker = ProjectSyncInvoker.DefaultProjectSyncInvoker(),
            formFactor = FormFactor.Mobile,
            category = Category.Activity,
            isLibrary = true
        )
        val modelWizard = ModelWizard.Builder()
            .addStep(
                ConfigureFeatureModuleStep(
                    model,
                    SdkVersionInfo.LOWEST_ACTIVE_API,
                    basePackage,
                    PluuPlugin.PLUU_MODULE
                )
            )
            .build()

        StudioWizardDialogBuilder(modelWizard, message("android.wizard.module.new.module.title"))
            .setProject(project)
            .setMinimumSize(JBUI.size(600, 400))
            .setPreferredSize(JBUI.size(600, 450))
            .build()
            .show()
    }
}