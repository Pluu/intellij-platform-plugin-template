package com.pluu.plugin.utils

import com.intellij.ide.actions.CreateDirectoryOrPackageAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory

object ModuleUtils {
    fun isRootPlace(dataContext: DataContext): Boolean {
        val project = dataContext.getData(CommonDataKeys.PROJECT)
        val module = dataContext.getData(LangDataKeys.MODULE_CONTEXT)
        return project != null && module != null
    }

    fun isAndroidModulePlace(dataContext: DataContext): Boolean {
        val module = dataContext.getData(PlatformCoreDataKeys.MODULE)
        return module != null && AndroidFacet.getInstance(module) != null
    }

    fun getSuggestedUnitTestDirectory(
        project: Project,
        srcModule: Module,
        filePackage: String
    ): PsiDirectory {
        val psiManager = PsiManager.getInstance(project)
        val rootManager = ModuleRootManager.getInstance(srcModule)
        val findTestDirectory = rootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE)
            .map { psiManager.findDirectory(it) }
            .firstOrNull()
        if (findTestDirectory != null) {
            return findTestDirectory
        }

        // Generate
        return generatedTestDirectory(
            directory = rootManager.getSourceRoots(JavaSourceRootType.SOURCE).first().toPsiDirectory(project)!!,
            filePackage = filePackage
        ) ?: throw IllegalStateException(">>>>")
    }

    private fun generatedTestDirectory(
        directory: PsiDirectory,
        filePackage: String
    ): PsiDirectory? {
        val path = CreateDirectoryOrPackageAction.EP.extensionList.asSequence()
            .flatMap { contributor ->
                contributor.getVariants(directory)
            }.firstOrNull {
                it.path.endsWith("test/java") || it.path.endsWith("test/kotlin")
            }?.path ?: return null

        val generatedDirectory =
            VfsUtil.createDirectories(path + "/" + filePackage.replace(".", "/"))
        return generatedDirectory.toPsiDirectory(directory.project)
    }
}