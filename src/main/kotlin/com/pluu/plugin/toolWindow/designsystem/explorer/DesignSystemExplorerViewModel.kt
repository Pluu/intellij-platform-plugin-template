package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.update.MergingUpdateQueue
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCache
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates

class DesignSystemExplorerViewModel(
    defaultFacet: AndroidFacet,
    private var contextFileForConfiguration: VirtualFile?,
    var supportedTypes: Array<DesignSystemType>
) : Disposable {

    private var listViewModel: DesignSystemExplorerListViewModel? = null

    //region ListModel update params
    private var refreshListModel: Boolean? = null
    private var listModelPattern: String? = null
    private var listModelResourceType: DesignSystemType? = null
    //endregion

    val filterOptions = FilterOptions.create(
        { refreshListModel() },
        { updateListModelSpeedSearch(it) }
    )

    private val listViewImageCache = ImageCache.createImageCache(
        parentDisposable = this,
        mergingUpdateQueue = MergingUpdateQueue(
            "queue",
            1000,
            true,
            MergingUpdateQueue.ANY_COMPONENT,
            this,
            null,
            false
        )
    )

    /**
     * View callback for when the DesignSystemType has changed.
     */
    var updateSupportTypeTabCallback: (() -> Unit) = {}

    /**
     * View callback whenever the resources lists needs to be repopulated.
     */
    var populateResourcesCallback: (() -> Unit) = {}

    /**
     * Callback called when the [AndroidFacet] has changed.
     */
    var facetUpdaterCallback: ((facet: AndroidFacet) -> Unit) = {}

    /**
     * Callback called when the current [DesignSystemType] has changed.
     */
    var designSystemTypeUpdaterCallback: ((resourceType: DesignSystemType) -> Unit) = {}

    var facet: AndroidFacet by Delegates.observable(defaultFacet) { _, oldFacet, newFacet ->
        if (newFacet != oldFacet) {
            contextFileForConfiguration = null // AndroidFacet changed, optional Configuration file is not valid.
            facetUpdaterCallback(newFacet)
            populateResourcesCallback()
        }
    }

    var supportTypeIndex: Int = 0
        set(value) {
            if (value != field && supportedTypes.indices.contains(value)) {
                field = value
                updateListModelDesignSystemType(supportedTypes[value])
                designSystemTypeUpdaterCallback(supportedTypes[value])
                updateSupportTypeTabCallback()
            }
        }

    fun createResourceListViewModel(): CompletableFuture<DesignSystemExplorerListViewModel> {
        (listViewModel as? Disposable)?.let { Disposer.dispose(it) }
        listViewModel = null
        return CompletableFuture.supplyAsync({
            DesignSystemExplorerListViewModelImpl(
                facet,
                contextFileForConfiguration,
                listViewImageCache,
                filterOptions,
                supportedTypes[supportTypeIndex]
            ).also {
                listViewModel = it
                it.facetUpdaterCallback = { newFacet -> this@DesignSystemExplorerViewModel.facet = newFacet }
                updateListModelIfNeeded()
            }
        }, EdtExecutorService.getInstance())
    }

    override fun dispose() {

    }

    //region ListModel update functions
    private fun refreshListModel() {
        val listModel = listViewModel
        if (listModel == null) {
            refreshListModel = true
        } else {
            listModel.updateUiCallback?.invoke(UpdateUiReason.DESIGN_SYSTEM_CHANGED)
        }
    }

    private fun updateListModelSpeedSearch(pattern: String) {
        val listModel = listViewModel
        if (listModel == null) {
            listModelPattern = pattern
        } else {
            listModel.speedSearch.updatePattern(pattern)
        }
    }

    private fun updateListModelDesignSystemType(designSystemType: DesignSystemType) {
        val listModel = listViewModel
        if (listModel == null) {
            listModelResourceType = designSystemType
        } else {
            listModel.currentDesignSystemType = designSystemType
        }
    }

    private fun updateListModelIfNeeded() {
        if (refreshListModel != null) {
            refreshListModel = null
            refreshListModel()
        }
        val pattern = listModelPattern
        if (pattern != null) {
            listModelPattern = null
            updateListModelSpeedSearch(pattern)
        }
        val resourceType = listModelResourceType
        if (resourceType != null) {
            listModelResourceType = null
            updateListModelDesignSystemType(resourceType)
        }
    }

    companion object {
        fun createViewModel(facet: AndroidFacet): DesignSystemExplorerViewModel {
            return DesignSystemExplorerViewModel(
                facet,
                null,
                DesignSystemType.values()
            )
        }
    }
}
