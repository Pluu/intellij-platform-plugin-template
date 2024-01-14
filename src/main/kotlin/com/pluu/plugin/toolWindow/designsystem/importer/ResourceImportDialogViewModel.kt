package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.ui.resourcemanager.plugin.DesignAssetRendererManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.js.inline.util.toIdentitySet
import java.awt.Image
import java.util.concurrent.CompletableFuture
import javax.swing.JTextField

const val MAX_IMPORT_FILES = 400

class ResourceImportDialogViewModel(
    val facet: AndroidFacet,
    assets: Sequence<DesignSystemItem>,
    val fileConfigurationViewModel: FileConfigurationViewModel = FileConfigurationViewModel(),
    private val designAssetImporter: DesignAssetImporter = DesignAssetImporter(),
    private val importersProvider: ImportersProvider = ImportersProvider(),
    private val importDoneCallback: () -> Unit
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

    private val rendererManager = DesignAssetRendererManager.getInstance()

    val fileCount: Int get() = assetSets.size

    var updateCallback: () -> Unit = {}

    private val fileViewModels = mutableMapOf<DesignSystemItem, FileImportRowViewModel>()

    fun commit() {
        designAssetImporter.importDesignAssets(assetSetsToImport, facet)
        importDoneCallback()
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

    fun getAssetPreview(asset: DesignSystemItem): CompletableFuture<out Image?> {
        return asset.file?.let { file ->
            rendererManager
                .getViewer(file)
                .getImage(file, facet.module, JBUI.size(150))
        } ?: CompletableFuture.completedFuture(null)
    }

    /**
     * Remove the [assetSet] from the list of [DesignAssetSet]s to import.
     * @return the [DesignAssetSet] that was containing the [assetSet]
     */
    fun removeAsset(assetSet: DesignAssetSet): DesignAssetSet {
        val designAssetSet = assetSetsToImport.first { it === assetSet }
        assetSetsToImport.remove(designAssetSet)
        updateCallback()
        return designAssetSet
    }

    /**
     * Creates a [FileImportRowViewModel] for the provided [asset].
     *
     * To let the [FileImportRowViewModel] delete itself, a callback needs to
     * be provided to notify its owner that it has been deleted, and the view needs to be
     * updated.
     */
    fun createFileViewModel(
        asset: DesignSystemItem,
        updateDesignSystemTypeCallback: (DesignSystemType) -> Unit,
        updateSampleCodeCallback: (String) -> Unit,
        removeCallback: () -> Unit
    ): FileImportRowViewModel {
        val viewModelRemoveCallback: () -> Unit = {
            removeCallback()
            fileViewModels.remove(asset)
        }

        val fileImportRowViewModel = FileImportRowViewModel(
            asset,
            updateDesignSystemTypeCallback = updateDesignSystemTypeCallback,
            updateSampleCodeCallback = updateSampleCodeCallback,
            removeCallback = viewModelRemoveCallback
        )
        fileViewModels[asset] = fileImportRowViewModel
        return fileImportRowViewModel
    }

    fun validateName(type: DesignSystemType, newName: String, field: JTextField? = null): ValidationInfo? {
        return when {
            newName.isEmpty() -> ValidationInfo("Cannot be empty", field)
            hasDuplicate(type, newName) -> createDuplicateValidationInfo(field)
            checkIfNameUnique(type, newName) -> getSameNameIsImportedValidationInfo(field)
            else -> null
        }
    }

    private fun hasDuplicate(type: DesignSystemType, newName: String): Boolean {
        // TODO: 선행 저장된 데이터 중복 체크
        return false
    }

    private fun createDuplicateValidationInfo(field: JTextField?) =
        ValidationInfo(
            "A component with this name already exists.",
            field
        ).asWarning()

    private fun getSameNameIsImportedValidationInfo(field: JTextField?) =
        ValidationInfo("A component with the same name and same type is also being imported.", field)
            .asWarning()

    private fun checkIfNameUnique(type: DesignSystemType, newName: String?): Boolean {
        var nameSeen = false
        return assetSetsToImport
            .any {
                if (it.name == newName && it.asset.type == type) {
                    if (nameSeen) return@any true
                    nameSeen = true
                }
                false
            }
    }

    private fun update(
        assetSet: DesignAssetSet,
        callback: (newAssetSet: DesignAssetSet) -> Unit,
        newAssetSetGenerator: (DesignAssetSet) -> DesignAssetSet
    ) {
        require(assetSetsToImport.contains(assetSet)) { "The assetSet \"${assetSet.name}\" should already exist" }
        val newAssetSet = newAssetSetGenerator(assetSet)
        assetSetsToImport.remove(assetSet)
        assetSetsToImport.add(newAssetSet)
        callback(newAssetSet)
    }

    /**
     * Creates a copy of [assetSet] with [newName] set as the [DesignAssetSet]'s name.
     * This method does not modify the underlying [DesignSystemItem], which is just passed to the newly
     * created [DesignAssetSet].
     *
     * [callback] is a callback with the old [assetSet] name and the newly created [DesignAssetSet].
     * This meant to be used by the view to update itself when it is holding a map from view to [DesignAssetSet].
     */
    fun updateName(
        assetSet: DesignAssetSet,
        newName: String,
        callback: (newAssetSet: DesignAssetSet) -> Unit
    ) {
        update(assetSet, callback) {
            DesignAssetSet(newName, assetSet.asset)
        }
    }

    fun updateDesignSystemType(
        assetSet: DesignAssetSet,
        newType: DesignSystemType,
        callback: (newAssetSet: DesignAssetSet) -> Unit
    ) {
        update(assetSet, callback) {
            assetSet.copy(asset = assetSet.asset.copy(type = newType))
        }
    }

    fun updateSampleCode(
        assetSet: DesignAssetSet,
        sampleCode: String,
        callback: (newAssetSet: DesignAssetSet) -> Unit
    ) {
        update(assetSet, callback) {
            assetSet.copy(asset = assetSet.asset.copy(sampleCode = sampleCode))
        }
    }

    fun updateAliasName(
        assetSet: DesignAssetSet,
        alisNames: List<String>,
        callback: (newAssetSet: DesignAssetSet) -> Unit
    ) {
        update(assetSet, callback) {
            assetSet.copy(asset = assetSet.asset.copy(aliasNames = alisNames))
        }
    }
}