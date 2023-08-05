package com.pluu.plugin.utils

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.codeStyle.JavaCodeStyleManagerImpl
import com.intellij.util.IncorrectOperationException
import com.pluu.plugin.utils.ModuleUtils.getSuggestedUnitTestDirectory
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile

fun generatorTestFile(file: PsiFile) {
    val project = file.project
    val srcModule = ModuleUtilCore.findModuleForFile(file)

    val packageName = when (file) {
        is KtFile -> file.packageFqName.toString()
        is PsiJavaFile -> file.packageName
        else -> throw IllegalStateException(">>>")
    }

    ApplicationManager.getApplication().runWriteAction {
        val testDirectory = getSuggestedUnitTestDirectory(
            project = project,
            srcModule = srcModule ?: throw IllegalArgumentException(""),
            filePackage = packageName
        )

        val psiFileFactory = PsiFileFactory.getInstance(project)
        val className = "${file.virtualFile.nameWithoutExtension}Test"
        @Language("kotlin") val testFile = psiFileFactory.createFileFromText(
            "${className}.kt",
            KotlinLanguage.INSTANCE,
            """
             package $packageName
             
             class $className {
                 // TODO: 여기를 잘 커스텀할 예정
             }
             """.trimIndent()
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