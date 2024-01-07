package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.resources.ResourceFolderType
import com.android.tools.idea.res.IdeResourceNameValidator
import com.android.tools.idea.res.StudioResourceRepositoryManager
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
    private val designAssetImporter: DesignAssetImporter = DesignAssetImporter(),
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

    private val rendererManager = DesignAssetRendererManager.getInstance()

    val fileCount: Int get() = assetSets.size

    var updateCallback: () -> Unit = {}

    private val fileViewModels = mutableMapOf<DesignSystemItem, FileImportRowViewModel>()

    fun commit() {
        designAssetImporter.importDesignAssets(assetSetsToImport, facet)
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

    /**
     * This validator only check for the name
     */
    private val resourceNameValidator = IdeResourceNameValidator.forFilename(ResourceFolderType.DRAWABLE, null)

    /**
     * We use a a separate validator for duplicate because if a duplicate is found, we just
     * want to show a warning - a user can override an existing resource.
     */
    private val resourceDuplicateValidator = IdeResourceNameValidator.forFilename(
        ResourceFolderType.DRAWABLE,
        null,
        StudioResourceRepositoryManager.getAppResources(facet)
    )

    fun getAssetPreview(asset: DesignSystemItem): CompletableFuture<out Image?> {
        return asset.file?.let { file ->
            rendererManager
                .getViewer(file)
                .getImage(file, facet.module, JBUI.size(50))
        } ?: CompletableFuture.completedFuture(null)
    }

    /**
     * Remove the [asset] from the list of [DesignSystemItem]s to import.
     * @return the [DesignAssetSet] that was containing the [asset]
     */
    fun removeAsset(asset: DesignSystemItem): DesignAssetSet {
        val designAssetSet = assetSetsToImport.first { it.asset == asset }
        assetSetsToImport.remove(designAssetSet)
        updateCallback()
        return designAssetSet
    }

    /**
     * Creates a copy of [assetSet] with [newName] set as the [DesignAssetSet]'s name.
     * This method does not modify the underlying [DesignSystemItem], which is just passed to the newly
     * created [DesignAssetSet].
     *
     * [assetRenamedCallback] is a callback with the old [assetSet] name and the newly created [DesignAssetSet].
     * This meant to be used by the view to update itself when it is holding a map from view to [DesignAssetSet].
     */
    fun rename(
        assetSet: DesignAssetSet,
        newName: String,
        assetRenamedCallback: (newAssetSet: DesignAssetSet) -> Unit
    ) {
        require(assetSetsToImport.contains(assetSet)) { "The assetSet \"${assetSet.name}\" should already exist" }
        val renamedAssetSet = DesignAssetSet(newName, assetSet.asset)
        assetSetsToImport.remove(assetSet)
        assetSetsToImport.add(renamedAssetSet)
        assetRenamedCallback(renamedAssetSet)
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
        removeCallback: (DesignSystemItem) -> Unit
    ): FileImportRowViewModel {
        val viewModelRemoveCallback: (DesignSystemItem) -> Unit = {
            removeCallback(asset)
            fileViewModels.remove(asset)
        }
        val fileImportRowViewModel =
            FileImportRowViewModel(asset, DesignSystemType.BUTTON, removeCallback = viewModelRemoveCallback)
        fileViewModels[asset] = fileImportRowViewModel
        return fileImportRowViewModel
    }

    fun validateName(newName: String, field: JTextField? = null): ValidationInfo? {
        val errorText = resourceNameValidator.getErrorText(newName)
        when {
            errorText != null -> return ValidationInfo(errorText, field)
            hasDuplicate(newName) -> return createDuplicateValidationInfo(field)
            checkIfNameUnique(newName) -> return getSameNameIsImportedValidationInfo(field)
            else -> return null
        }
    }

    private fun hasDuplicate(newName: String) = resourceDuplicateValidator.doesResourceExist(newName)

    private fun createDuplicateValidationInfo(field: JTextField?) =
        ValidationInfo(
            "A resource with this name already exists and might be overridden if the qualifiers are the same.",
            field
        ).asWarning()

    private fun getSameNameIsImportedValidationInfo(field: JTextField?) =
        ValidationInfo("A resource with the same name is also being imported.", field)
            .asWarning()

    private fun checkIfNameUnique(newName: String?): Boolean {
        var nameSeen = false
        return assetSetsToImport
            .any {
                if (it.name == newName) {
                    if (nameSeen) return@any true
                    nameSeen = true
                }
                false
            }
    }
}