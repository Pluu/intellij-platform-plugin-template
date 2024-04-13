package com.pluu.plugin.utils

import com.intellij.codeInsight.navigation.openFileWithPsiElement
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.codeStyle.JavaCodeStyleManagerImpl
import com.intellij.util.IncorrectOperationException
import com.pluu.plugin.utils.ModuleUtils.getSuggestedAndroidTestDirectory
import com.pluu.plugin.utils.ModuleUtils.getSuggestedTestDirectory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import java.util.*

fun generatorUnitTestFile(
    srcPackage: String,
    srcModule: Module,
    srcClassName: String
) {
    val project = srcModule.project
    val className = "${srcClassName}Test"

    ApplicationManager.getApplication().runWriteAction {
        val testDirectory = getSuggestedTestDirectory(
            srcModule = srcModule
        ).let { rootDirectory ->
            VfsUtil.createDirectories(rootDirectory.path + "/" + srcPackage.replace(".", "/"))
        }?.toPsiDirectory(project) ?: throw IllegalStateException("Failed to create a test folder")

        val template = FileTemplateManager.getInstance(project).getJ2eeTemplate("ViewModelTest")
        val templateProperties = Properties().apply {
            setProperty("PACKAGE_NAME", srcPackage)
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

        openFileWithPsiElement(createdFile, searchForOpen = true, requestFocus = true)
    }
}

fun generatorAndroidTestFile(
    srcPackage: String,
    srcModule: Module,
    srcClassName: String,
    writeFileAction: () -> String
) {
    val project = srcModule.project
    val className = "${srcClassName}Test"

    ApplicationManager.getApplication().runWriteAction {
        val testDirectory = getSuggestedAndroidTestDirectory(
            srcModule = srcModule
        ).let { rootDirectory ->
            VfsUtil.createDirectories(rootDirectory.path + "/" + srcPackage.replace(".", "/"))
        }?.toPsiDirectory(project) ?: throw IllegalStateException("Failed to create a test folder")

        val testFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "${className}.kt",
                KotlinLanguage.INSTANCE,
                writeFileAction()
            )

        JavaCodeStyleManagerImpl(project).optimizeImports(testFile)
        val createdFile: PsiElement = try {
            testDirectory.add(testFile)
        } catch (e: IncorrectOperationException) {
            return@runWriteAction
        }

        openFileWithPsiElement(createdFile, searchForOpen = true, requestFocus = true)
    }
}