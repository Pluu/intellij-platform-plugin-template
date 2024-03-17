package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.openapi.project.Project
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemTab
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.provider.DesignSystemManager
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManagerImpl
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCache
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import kotlin.properties.Delegates

class DesignSystemExplorerListViewModelImpl(
    override val project: Project,
    private val listViewImageCache: ImageCache,
    override val filterOptions: FilterOptions,
    initialDesignSystemTab: DesignSystemTab,
    selectAssetAction: ((asset: DesignSystemItem) -> Unit)? = null,
) : DesignSystemExplorerListViewModel {

    /**
     * callback called when the resource model have change. This happen when the facet is changed.
     */
    override var updateUiCallback: ((UpdateUiReason) -> Unit)? = null

    override var facetUpdaterCallback: ((facet: AndroidFacet) -> Unit)? = null

    override var currentTab: DesignSystemTab by Delegates.observable(initialDesignSystemTab) { _, oldValue, newValue ->
        if (newValue != oldValue) {
            updateUiCallback?.invoke(UpdateUiReason.DESIGN_SYSTEM_TYPE_CHANGED)
        }
    }

//    private val dataManager = ResourceDataManager(project)

    override val selectedTabName: String get() = currentTab.name

    override val speedSearch = SpeedSearch(true).apply {
        if (filterOptions.searchString.isNotEmpty()) {
            updatePattern(filterOptions.searchString)
        }
    }

    override val assetPreviewManager: DesignAssetPreviewManager =
        DesignAssetPreviewManagerImpl(listViewImageCache)

    override fun clearCacheForCurrentResources() {
        getDesignAssetSets().whenCompleteAsync({ lists, throwable ->
            if (throwable == null) {
                for (item in lists) {
                    clearImageCache(item.asset)
                }
                updateUiCallback?.invoke(UpdateUiReason.IMAGE_CACHE_CHANGED)
            }
        }, EdtExecutorService.getInstance())
    }

    override fun clearImageCache(asset: DesignSystemItem) {
        listViewImageCache.clear(asset)
    }

    override fun getDesignAssetSets() = resourceExplorerSupplyAsync {
        getResourceSections(project)
    }

    private fun getResourceSections(project: Project): List<DesignAssetSet> {
        val designSystemType = currentTab.filterType
        return DesignSystemManager.getModuleResources(project, designSystemType)
            .sortedBy { it.name }
            .map {
                DesignAssetSet(it.name, it)
            }
    }
}

/**
 * Common wrapper for methods that returns resource information in a [CompletableFuture]. Makes sure the method is run in a background
 * thread for long-running operations.
 */
private fun <T> resourceExplorerSupplyAsync(runnable: () -> T): CompletableFuture<T> =
    supplyAsync({
        runnable()
    }, AppExecutorUtil.getAppExecutorService())