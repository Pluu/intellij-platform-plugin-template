package com.pluu.plugin.toolWindow.designsystem.importer

import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.js.inline.util.toIdentitySet

const val MAX_IMPORT_FILES = 400

class ResourceImportDialogViewModel(
    val facet: AndroidFacet,
    assets: Sequence<DesignSystemItem>,
    private val importersProvider: ImportersProvider = ImportersProvider()
) {
    /**
     *  The [DesignSystemItem]s to be imported.
     *
     *  They are stored in an IdentitySet because there might
     *  be conflicts when a [DesignSystemItem] is being renamed with a name similar
     *  to another [DesignSystemItem] being imported.
     */
    private val assetSetsToImport = assets
        .take(MAX_IMPORT_FILES)
        .groupIntoDesignAssetSet()
        .toIdentitySet()

    val assetSets get() = assetSetsToImport

    val fileCount: Int get() = assetSets.size

    var updateCallback: () -> Unit = {}

    /**
     * Passes the [assetSetsToImport] to the [SummaryScreenViewModel].
     * @see summaryScreenViewModel
     */
    fun commit() {
//        summaryScreenViewModel.assetSetsToImport = assetSetsToImport
    }

    /**
     * Invoke a path chooser and add new files to the list of assets to import.
     * If a file is already present, it won't be added and new [DesignSystemItem] will be merged
     * with [DesignAssetSet] of the same name.
     *
     * [assetAddedCallback] will be called with a new or existing [DesignAssetSet] and
     * a list of newly added [DesignSystemItem]s, which means that it's a subset of [DesignAssetSet.asset].
     * This allows the view to merge the new [DesignSystemItem] within a potential existing view of a [DesignAssetSet].
     * The callback won't be called if there is no new file.
     */
    fun importMoreAssets(assetAddedCallback: (DesignAssetSet, DesignSystemItem) -> Unit) {
        val assetByName = assetSetsToImport.associateBy { it.name }
        chooseDesignAssets(importersProvider) { newAssetSets ->
            newAssetSets
                .take(MAX_IMPORT_FILES)
                .groupIntoDesignAssetSet()
                .forEach {
                    addAssetSet(assetByName, it, assetAddedCallback)
                }
        }
    }

    /**
     * Same as [importMoreAssets] but only if the list of asset to be imported is currently empty.
     */
    fun importMoreAssetIfEmpty(assetAddedCallback: (DesignAssetSet, DesignSystemItem) -> Unit) {
        if (assetSets.isEmpty()) {
            importMoreAssets(assetAddedCallback)
        }
    }

    /**
     * Add the [assetSet] to the list of asset to be imported.
     * If [existingAssets] contains a [DesignAssetSet] with the same name as [assetSet],
     * the [assetSet] will be added the existing [DesignAssetSet], otherwise a new one
     * will be created.
     * @param existingAssets A map from a [DesignAssetSet]'s name to the [DesignAssetSet].
     * This is used to avoid iterating through the whole [assetSetsToImport] set to try find
     * a [DesignAssetSet] with the same name.
     */
    private fun addAssetSet(
        existingAssets: Map<String, DesignAssetSet>,
        assetSet: DesignAssetSet,
        assetAddedCallback: (DesignAssetSet, DesignSystemItem) -> Unit
    ) {
        val existingAssetSet = existingAssets[assetSet.name]
        if (existingAssetSet != null) {
            val existingPaths = existingAssetSet.asset.file?.path
            val onlyNewFiles = assetSet.asset.file?.path != existingPaths
            if (onlyNewFiles) {
                assetAddedCallback(
                    existingAssetSet.copy(asset = assetSet.asset), assetSet.asset
                )
                updateCallback()
            }
        } else {
            assetSetsToImport.add(assetSet)
            assetAddedCallback(assetSet, assetSet.asset)
            updateCallback()
        }
    }
}