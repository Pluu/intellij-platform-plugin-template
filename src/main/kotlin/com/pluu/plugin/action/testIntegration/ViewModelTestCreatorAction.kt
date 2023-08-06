package com.pluu.plugin.action.testIntegration

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.utils.generatorUnitTestFile
import org.jetbrains.kotlin.idea.core.getFqNameByDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class ViewModelTestCreatorAction : AnAction(PluuPlugin.TestAction.CreateViewModel) {

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.project ?: return
        val module = event.getData(LangDataKeys.MODULE) ?: return

        val psiFile = event.getData(LangDataKeys.VIRTUAL_FILE)?.toPsiFile(project) ?: return
        val element = getElement(editor, psiFile) ?: return

        ApplicationManager.getApplication().runWriteAction {
            generatorUnitTestFile(
                srcPackage = psiFile.getFqNameByDirectory().toString(),
                srcModule = module,
                srcClassName = element.text
            )
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project ?: return
        val psiFile = event.getData(LangDataKeys.VIRTUAL_FILE)?.toPsiFile(project) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val element = getElement(editor, psiFile) ?: return
        event.presentation.isVisible = element.text.endsWith("ViewModel")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun getElement(editor: Editor, file: PsiFile): PsiElement? {
        val caretModel = editor.caretModel
        val position = caretModel.offset
        return file.findElementAt(position)
    }
}