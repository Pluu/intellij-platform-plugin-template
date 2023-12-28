package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

/** Returns the [PsiFile] corresponding to the source of the given resource item, if possible.  */
fun getItemPsiFile(project: Project, item: DesignSystemItem): PsiFile? {
    if (project.isDisposed) {
        return null
    }
    val virtualFile = item.file
    val psiManager = PsiManager.getInstance(project)
    return psiManager.findFile(virtualFile)
}
