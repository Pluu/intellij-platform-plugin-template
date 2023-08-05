package com.pluu.plugin.action.testIntegration

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.io.FileUtil
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.utils.generatorTestFile

class ViewModelTestCreatorAction : AnAction(PluuPlugin.TestAction.CreateViewModel) {

    override fun actionPerformed(event: AnActionEvent) {
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        ApplicationManager.getApplication().runWriteAction {
            generatorTestFile(file = psiFile)
        }
    }

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            val fileName = event.getData(CommonDataKeys.VIRTUAL_FILE)?.name ?: return
            val fileNameWithoutExtension = FileUtil.getNameWithoutExtension(fileName)
            event.presentation.isVisible = fileNameWithoutExtension.endsWith("ViewModel")
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}