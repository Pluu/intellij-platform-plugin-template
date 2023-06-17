package com.pluu.plugin.action

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.pluu.plugin.FileTemplateProviderImpl
import org.jetbrains.kotlin.idea.KotlinIcons

class ViewModelCreateFileFromTemplateAction : CreateFileFromTemplateAction(
    "Create ViewModel",
    "create just ViewModel class file",
    null
), DumbAware {
    override fun startInWriteAction(): Boolean = false

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder.setTitle("New ViewModel")
            .addKind("ViewModel file", KotlinIcons.CLASS, FileTemplateProviderImpl.PLUU_VIEW_MODEL)
    }

    override fun getActionName(
        directory: PsiDirectory,
        newName: String,
        templateName: String
    ): String = "New ViewModel"

}