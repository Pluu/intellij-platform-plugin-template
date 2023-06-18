package com.pluu.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.pluu.plugin.utils.ModuleUtils
import icons.StudioIcons

class FeatureModuleCreateWizardAction : AnAction(
    "Create feature module", null, StudioIcons.Shell.Filetree.ANDROID_MODULE
) {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = ModuleUtils.isRootPlace(e.dataContext)
    }

    override fun actionPerformed(p0: AnActionEvent) {
        TODO("Not yet implemented")
    }
}