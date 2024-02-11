package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.util.toIoFile
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.provider.DesignSystemManager
import com.pluu.plugin.toolWindow.designsystem.provider.sampleDirName

class DesignAssetImporter {
    fun importDesignAssets(
        assetSets: Set<DesignAssetSet>,
        project: Project,
        isNeedImportImageAssert: Boolean
    ) {
        if (assetSets.isEmpty()) return

        val sampleRoot = DesignSystemManager.getOrCreateDefaultRootDirectory(project)
        LocalFileSystem.getInstance().refreshIoFiles(listOf(sampleRoot.toIoFile()))

        val groupedAssets = assetSets.groupBy {
            it.asset.type
        }
        WriteCommandAction.runWriteCommandAction(project, "Write Samples", null, {
            groupedAssets.forEach { (designSystemType, items) ->
                if (isNeedImportImageAssert) {
                    copyAssetsInFolder(designSystemType, items, sampleRoot)
                }
                DesignSystemManager.saveSample(project, designSystemType, items)
            }
        })
    }

    fun removeDesignAsset(
        item: DesignSystemItem,
        project: Project,
        isRemoveThumbnail: Boolean
    ) {
        val sampleRoot = DesignSystemManager.getOrCreateDefaultRootDirectory(project)
        LocalFileSystem.getInstance().refreshIoFiles(listOf(sampleRoot.toIoFile()))
        DesignSystemManager.removeSample(project, item.type, item)
        if (isRemoveThumbnail) {
            removeThumbnail(item.file, project)
        }
    }

    fun removeThumbnail(
        file: VirtualFile?,
        project: Project
    ) {
        val sampleRoot = DesignSystemManager.getOrCreateDefaultRootDirectory(project)
        LocalFileSystem.getInstance().refreshIoFiles(listOf(sampleRoot.toIoFile()))
        WriteCommandAction.runWriteCommandAction(project, "Remove Thumbnail", null, {
            // Delete thumbnail
            val safeFile = file?.toIoFile() ?: return@runWriteCommandAction
            safeFile.delete()
        })
    }

    fun renameThumbnail(
        file: VirtualFile?,
        rename: String,
        project: Project
    ) {
        val safeFile = file ?: return
        WriteCommandAction.runWriteCommandAction(project, "Rename Thumbnail", null, {
            safeFile.rename(this, rename)
        })
    }

    private fun copyAssetsInFolder(
        type: DesignSystemType,
        importingAsset: List<DesignAssetSet>,
        rootSample: VirtualFile
    ) {
        val folder = VfsUtil.createDirectoryIfMissing(rootSample, type.sampleDirName)
        importingAsset.forEach {
            val file = it.asset.file ?: return@forEach
            file.copy(this, folder, it.asset.fileNameWithExtension)
        }
    }
}
