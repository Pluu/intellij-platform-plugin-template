package com.pluu.plugin.action

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.pluu.plugin.FileTemplateProviderImpl
import com.pluu.plugin.utils.ModuleUtils
import icons.StudioIcons
import org.jetbrains.kotlin.idea.KotlinIcons

class ViewModelCreateFileFromTemplateAction : CreateFileFromTemplateAction(
    "Create ViewModel",
    "create just ViewModel class file",
    StudioIcons.Shell.Filetree.ANDROID_FILE
) {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = ModuleUtils.isAndroidModulePlace(e.dataContext)
    }

    override fun createFile(name: String, templateName: String, dir: PsiDirectory): PsiFile? {
        return super.createFile("${name}ViewModel", templateName, dir)
    }

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder.setTitle("New ViewModel")
            .addKind("ViewModel file", KotlinIcons.CLASS, FileTemplateProviderImpl.PLUU_VIEW_MODEL)
            .setValidator(ViewModelNameValidator())
    }

    override fun getActionName(
        directory: PsiDirectory,
        newName: String,
        templateName: String
    ): String = "New ViewModel"

    private class ViewModelNameValidator : InputValidatorEx {
        override fun canClose(inputString: String) = checkInput(inputString)

        override fun getErrorText(inputString: String) = "Doesn't support the \"ViewModel\" suffix."

        override fun checkInput(inputString: String) =
            inputString.lowercase().endsWith("viewmodel").not()
    }

}