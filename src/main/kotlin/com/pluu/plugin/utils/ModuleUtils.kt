package com.pluu.plugin.utils

import com.android.AndroidProjectTypes
import com.android.tools.idea.gradle.util.GradleProjectSystemUtil.findGradleBuildFile
import com.android.tools.idea.project.AndroidProjectInfo
import com.android.tools.idea.projectsystem.gradle.getAndroidTestModule
import com.android.tools.idea.projectsystem.gradle.getUnitTestModule
import com.intellij.ide.actions.CreateDirectoryOrPackageAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.pluu.plugin.PluuPlugin
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.idea.util.sourceRoots
import java.io.File

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

    fun getSuggestedTestDirectory(
        srcModule: Module
    ): VirtualFile = getSuggestedTestDirectory(
        srcModule,
        testModuleProvider = { module ->
            module
        },
        findTestRoot = { module ->
            module.getUnitTestModule()?.sourceRoots?.firstOrNull()
        },
        createDirectory = { module ->
            val directory = module.rootManager.getSourceRoots(JavaSourceRootType.SOURCE).first()
                .toPsiDirectory(module.project)!!
            createTestDirectory(directory) { path ->
                path.endsWith("test/java") || path.endsWith("test/kotlin")
            }
        }
    )

    fun getSuggestedAndroidTestDirectory(
        srcModule: Module
    ): VirtualFile = getSuggestedTestDirectory(
        srcModule,
        testModuleProvider = { module ->
            module
//            AndroidFacet.getInstance(module.project.findAppModule()!!)?.androidTestModule!!
        },
        findTestRoot = { module ->
            module.getAndroidTestModule()!!
                .rootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).firstOrNull()
//            module.rootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).firstOrNull()
        },
        createDirectory = { module ->
            val directory = module.rootManager.getSourceRoots(JavaSourceRootType.SOURCE).first()
                .toPsiDirectory(module.project)!!
//            val directory = module.project.findAppModule()!!
//                .guessModuleDir()!!
//                .toPsiDirectory(module.project)!!
            createTestDirectory(directory) { path ->
                path.endsWith("androidTest/java") || path.endsWith("androidTest/kotlin")
            }
        }
    )

    private fun getSuggestedTestDirectory(
        srcModule: Module,
        testModuleProvider: (Module) -> Module,
        findTestRoot: (Module) -> VirtualFile?,
        createDirectory: (Module) -> VirtualFile?
    ): VirtualFile {
        val testModule = testModuleProvider(srcModule)
        val testRootUrls = findTestRoot(testModule)
        if (testRootUrls != null) {
            return testRootUrls
        }

        // Generate
        return createDirectory(testModule) ?: throw IllegalStateException("Failed to create a test folder")
    }

    private fun createTestDirectory(
        directory: PsiDirectory,
        matchers: (String) -> Boolean
    ): VirtualFile? {
        val path = CreateDirectoryOrPackageAction.EP.extensionList.asSequence()
            .flatMap { contributor ->
                contributor.getVariants(directory)
            }
            .filter {
                it.rootType?.isForTests == true
            }
            .firstOrNull { matchers(it.path) }
            ?.path ?: return null

        return VfsUtil.createDirectories(path)
    }

    private fun Project.findAppModule(): Module? {
        return AndroidProjectInfo.getInstance(this)
            .getAllModulesOfProjectType(AndroidProjectTypes.PROJECT_TYPE_APP)
            .firstOrNull()
    }

    fun useConventionPlugin(moduleRootDir: File): Boolean {
        val rootVirtualFile = moduleRootDir.toVirtualFile() ?: return false
        val buildFile = findGradleBuildFile(rootVirtualFile) ?: return false
        if (!buildFile.exists()) return false
        val buildFileText = VfsUtil.loadText(buildFile)
        return buildFileText.contains(PluuPlugin.Convension.LIBRARY)
    }

    fun baseModuleName(module: Module): String {
        return module.name.split(".").drop(1).joinToString(separator = ":")
    }
}