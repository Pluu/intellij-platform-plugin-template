package com.pluu.plugin.action.testIntegration

import com.intellij.ide.fileTemplates.FileTemplateManager
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
import com.pluu.plugin.utils.generatorAndroidTestFile
import org.jetbrains.kotlin.idea.core.getFqNameByDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import java.util.*

class ActivityUiTestCreatorAction : AnAction(PluuPlugin.TestAction.CreateActivity) {

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.project ?: return
        val module = event.getData(LangDataKeys.MODULE) ?: return

        val psiFile = event.getData(LangDataKeys.VIRTUAL_FILE)?.toPsiFile(project) ?: return
        val element = getElement(editor, psiFile) ?: return

        ApplicationManager.getApplication().runWriteAction {
            val srcPackage = psiFile.getFqNameByDirectory().toString()
            val srcClassName = element.text
            generatorAndroidTestFile(
                srcPackage = srcPackage,
                srcModule = module,
                srcClassName = srcClassName
            ) {
                val template = FileTemplateManager.getInstance(project).getJ2eeTemplate("ActivityTest")
                val templateProperties = Properties().apply {
                    setProperty("PACKAGE_NAME", srcPackage)
                    setProperty("NAME", srcClassName)
                }
                template.getText(templateProperties)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project ?: return
        val psiFile = event.getData(LangDataKeys.VIRTUAL_FILE)?.toPsiFile(project) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val element = getElement(editor, psiFile) ?: return
        event.presentation.isVisible = element.text.endsWith("Activity")
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