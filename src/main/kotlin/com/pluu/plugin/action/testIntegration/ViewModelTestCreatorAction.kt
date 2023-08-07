package com.pluu.plugin.action.testIntegration

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.utils.ModuleUtils.getSuggestedTestDirectory
import org.jetbrains.kotlin.idea.KotlinLanguage
import java.util.*

class ViewModelTestCreatorAction : AbstractTestCreatorAction(PluuPlugin.TestAction.CreateViewModel) {
    override fun isAvailable(fileName: String): Boolean {
        return fileName.endsWith("ViewModel")
    }

    override fun getSuggestedDirectory(srcModule: Module): VirtualFile {
        return getSuggestedTestDirectory(srcModule = srcModule)
    }

    override fun createFile(project: Project, srcPackage: String, srcClassName: String): PsiFile {
        val template = FileTemplateManager.getInstance(project).getJ2eeTemplate("ViewModelTest")
        val templateProperties = Properties().apply {
            setProperty("PACKAGE_NAME", srcPackage)
            setProperty("NAME", srcClassName)
        }
        return PsiFileFactory.getInstance(project)
            .createFileFromText(
                "${srcClassName}Test.kt",
                KotlinLanguage.INSTANCE,
                template.getText(templateProperties)
            )
    }
}