package com.pluu.plugin.action

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.wizard.model.ModelWizard
import com.android.tools.idea.wizard.ui.StudioWizardDialogBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.ui.JBUI
import com.pluu.plugin.PluuBundle
import com.pluu.plugin.module.feature.NewFeatureModuleModel
import com.pluu.plugin.module.sample.ConfigureFeatureSampleModuleStep
import com.pluu.plugin.utils.ModuleUtils
import icons.PluuIcons
import org.jetbrains.android.util.AndroidBundle.message

class FeatureSampleModuleCreateWizardAction : AnAction(
    PluuBundle.message("pluu.module.new.feature.sample.title"),
    PluuBundle.message("pluu.module.new.feature.sample.description"),
    PluuIcons.Konata
) {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = ModuleUtils.isRootPlace(e.dataContext)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val model = NewFeatureModuleModel.fromExistingProject(
            project = project,
            projectSyncInvoker = ProjectSyncInvoker.DefaultProjectSyncInvoker(),
            moduleParent = ":sample",
            isLibrary = false,
            isNeedBaseModule = true,
        )
        val modelWizard = ModelWizard.Builder()
            .addStep(ConfigureFeatureSampleModuleStep(model))
            .build()

        StudioWizardDialogBuilder(modelWizard, message("android.wizard.module.new.module.title"))
            .setProject(project)
            .setMinimumSize(JBUI.size(600, 450))
            .setPreferredSize(JBUI.size(600, 500))
            .build()
            .show()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}