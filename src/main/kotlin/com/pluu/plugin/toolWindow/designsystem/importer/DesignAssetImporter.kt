package com.pluu.plugin.toolWindow.designsystem.importer

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.provider.DesignSystemManager
import com.pluu.plugin.toolWindow.designsystem.provider.sampleDirName
import org.jetbrains.android.facet.AndroidFacet

class DesignAssetImporter {
    fun importDesignAssets(
        assetSets: Set<DesignAssetSet>,
        facet: AndroidFacet
    ) {
        val sampleRoot = DesignSystemManager.getOrCreateDefaultRootDirectory(facet)

        val groupedAssets = assetSets.groupBy {
            it.asset.type
        }
        WriteCommandAction.runWriteCommandAction(facet.module.project, "Write samples", null, {
            groupedAssets.forEach { (designSystemType, items) ->
                copyAssetsInFolder(designSystemType, items, sampleRoot)
                DesignSystemManager.saveSample(facet, designSystemType, items)
            }
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
            file.copy(this, folder, "${it.name}.${file.extension}")
        }
    }
}
