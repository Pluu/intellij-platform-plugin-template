package com.pluu.plugin.utils

import com.intellij.ide.actions.CreateDirectoryOrPackageAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.testIntegration.createTest.CreateTestUtils
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
        srcModule: Module
    ): VirtualFile {
        val testModule = AndroidFacet.getInstance(srcModule)?.unitTestModule
        if (testModule != null) {
            val testRootUrls = CreateTestUtils.computeTestRoots(testModule).firstOrNull()
            if (testRootUrls != null) {
                return testRootUrls
            }
        }

        // Generate
        val project = srcModule.project
        val rootManager = ModuleRootManager.getInstance(srcModule)
        return generatedUnitTestDirectory(
            directory = rootManager.getSourceRoots(JavaSourceRootType.SOURCE).first().toPsiDirectory(project)!!
        ) ?: throw IllegalStateException("Failed to create a test folder")
    }

    private fun generatedUnitTestDirectory(
        directory: PsiDirectory
    ): VirtualFile? {
        val path = CreateDirectoryOrPackageAction.EP.extensionList.asSequence()
            .flatMap { contributor ->
                contributor.getVariants(directory)
            }.filter { it.rootType?.isForTests == true }.firstOrNull {
                it.path.endsWith("test/java") || it.path.endsWith("test/kotlin")
            }?.path ?: return null

        return VfsUtil.createDirectories(path)
    }
}