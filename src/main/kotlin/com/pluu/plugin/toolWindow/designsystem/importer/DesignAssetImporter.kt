package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.util.toIoFile
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.provider.DesignSystemManager
import com.pluu.plugin.toolWindow.designsystem.provider.sampleDirName
import org.jetbrains.android.facet.AndroidFacet

class DesignAssetImporter {
    fun importDesignAssets(
        assetSets: Set<DesignAssetSet>,
        facet: AndroidFacet,
        isNeedImportImageAssert: Boolean
    ) {
        if (assetSets.isEmpty()) return

        val sampleRoot = DesignSystemManager.getOrCreateDefaultRootDirectory(facet)
        LocalFileSystem.getInstance().refreshIoFiles(listOf(sampleRoot.toIoFile()))

        val groupedAssets = assetSets.groupBy {
            it.asset.type
        }
        WriteCommandAction.runWriteCommandAction(facet.module.project, "Write Samples", null, {
            groupedAssets.forEach { (designSystemType, items) ->
                if (isNeedImportImageAssert) {
                    copyAssetsInFolder(designSystemType, items, sampleRoot)
                }
                DesignSystemManager.saveSample(facet, designSystemType, items)
            }
        })
    }

    fun removeDesignAsset(
        item: DesignSystemItem,
        facet: AndroidFacet,
        isRemoveThumbnail: Boolean
    ) {
        val sampleRoot = DesignSystemManager.getOrCreateDefaultRootDirectory(facet)
        LocalFileSystem.getInstance().refreshIoFiles(listOf(sampleRoot.toIoFile()))
        DesignSystemManager.removeSample(facet, item.type, item)
        if (isRemoveThumbnail) {
            removeThumbnail(item, facet)
        }
    }

    fun removeThumbnail(
        item: DesignSystemItem,
        facet: AndroidFacet
    ) {
        val sampleRoot = DesignSystemManager.getOrCreateDefaultRootDirectory(facet)
        LocalFileSystem.getInstance().refreshIoFiles(listOf(sampleRoot.toIoFile()))
        WriteCommandAction.runWriteCommandAction(facet.module.project, "Remove Thumbnail", null, {
            // Delete thumbnail
            val file = item.file?.toIoFile() ?: return@runWriteCommandAction
            file.delete()
        })
    }

    fun renameThumbnail(
        file: VirtualFile?,
        rename: String,
        facet: AndroidFacet
    ) {
        val safeFile = file ?: return
        WriteCommandAction.runWriteCommandAction(facet.module.project, "Rename Thumbnail", null, {
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
