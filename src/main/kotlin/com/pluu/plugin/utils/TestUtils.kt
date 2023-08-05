package com.pluu.plugin.utils

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.codeStyle.JavaCodeStyleManagerImpl
import com.intellij.util.IncorrectOperationException
import com.pluu.plugin.utils.ModuleUtils.getSuggestedUnitTestDirectory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

fun generatorTestFile(file: PsiFile) {
    val project = file.project
    val srcModule = ModuleUtilCore.findModuleForFile(file)

    val packageName = when (file) {
        is KtFile -> file.packageFqName.toString()
        is PsiJavaFile -> file.packageName
        else -> throw IllegalStateException(">>>")
    }
    val className = "${file.virtualFile.nameWithoutExtension}Test"

    ApplicationManager.getApplication().runWriteAction {
        val testDirectory = getSuggestedUnitTestDirectory(
            project = project,
            srcModule = srcModule ?: throw IllegalArgumentException(""),
            filePackage = packageName
        )

        val template = FileTemplateManager.getInstance(project).getJ2eeTemplate("ViewModelTest")
        val templateProperties = Properties().apply {
            setProperty("PACKAGE_NAME", packageName)
            setProperty("NAME", className)
        }
        val testFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "${className}.kt",
                KotlinLanguage.INSTANCE,
                template.getText(templateProperties)
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