package com.pluu.plugin.action.testIntegration

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.codeStyle.JavaCodeStyleManagerImpl
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.core.getFqNameByDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile

abstract class AbstractTestCreatorAction(actionName: String) : AnAction(actionName) {

    final override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.project ?: return
        val module = event.getData(LangDataKeys.MODULE) ?: return

        val psiFile = event.getData(LangDataKeys.VIRTUAL_FILE)?.toPsiFile(project) ?: return
        val element = getElement(editor, psiFile) ?: return

        val srcClassName = element.text
        val generateClassName = "${srcClassName}Test"
        val srcPackage = psiFile.getFqNameByDirectory().toString()

        ApplicationManager.getApplication().runWriteAction {
            val testDirectory = getSuggestedDirectory(
                srcModule = module
            ).let { rootDirectory ->
                VfsUtil.createDirectories(rootDirectory.path + "/" + srcPackage.replace(".", "/"))
            }?.toPsiDirectory(project) ?: throw IllegalStateException("Failed to create a test folder")

            val testFile = createFile(
                project, srcPackage, generateClassName
            )
            JavaCodeStyleManagerImpl(project).optimizeImports(testFile)
            val createdFile: PsiElement = try {
                testDirectory.add(testFile)
            } catch (e: IncorrectOperationException) {
                return@runWriteAction
            }

            NavigationUtil.openFileWithPsiElement(createdFile, true, true)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project ?: return
        val psiFile = event.getData(LangDataKeys.VIRTUAL_FILE)?.toPsiFile(project) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val element = getElement(editor, psiFile) ?: return
        event.presentation.isVisible = isAvailable(element.text)
    }

    final override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    protected fun getElement(editor: Editor, file: PsiFile): PsiElement? {
        val caretModel = editor.caretModel
        val position = caretModel.offset
        return file.findElementAt(position)
    }

    protected abstract fun isAvailable(fileName: String): Boolean

    protected abstract fun getSuggestedDirectory(srcModule: Module): VirtualFile

    protected abstract fun createFile(project: Project, srcPackage: String, generateClassName: String): PsiFile
}