package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.findCompatibleFacets
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSection
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.model.getModuleResources
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManagerImpl
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCache
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import kotlin.properties.Delegates

class DesignSystemExplorerListViewModelImpl(
    override val facet: AndroidFacet,
    private val contextFile: VirtualFile?,
    private val listViewImageCache: ImageCache,
    override val filterOptions: FilterOptions,
    designSystemType: DesignSystemType
) : DesignSystemExplorerListViewModel {

    /**
     * callback called when the resource model have change. This happen when the facet is changed.
     */
    override var updateUiCallback: ((UpdateUiReason) -> Unit)? = null

    override var facetUpdaterCallback: ((facet: AndroidFacet) -> Unit)? = null

    override var currentDesignSystemType: DesignSystemType by Delegates.observable(designSystemType) { _, oldValue, newValue ->
        if (newValue != oldValue) {
            updateUiCallback?.invoke(UpdateUiReason.DESIGN_SYSTEM_TYPE_CHANGED)
        }
    }

    override val speedSearch = SpeedSearch(true).apply {
        if (filterOptions.searchString.isNotEmpty()) {
            updatePattern(filterOptions.searchString)
        }
    }

    override val assetPreviewManager: DesignAssetPreviewManager =
        DesignAssetPreviewManagerImpl(facet, listViewImageCache, contextFile)

    override fun clearCacheForCurrentResources() {
        getCurrentModuleResourceLists().whenCompleteAsync({ lists, throwable ->
            if (throwable == null) {
                lists.flatMap {
                    it.assetSets.map { it.asset }
                }.forEach(::clearImageCache)
                updateUiCallback?.invoke(UpdateUiReason.IMAGE_CACHE_CHANGED)
            }
        }, EdtExecutorService.getInstance())
    }

    override fun clearImageCache(asset: DesignSystemItem) {
        listViewImageCache.clear(asset)
    }

    override fun getCurrentModuleResourceLists() = resourceExplorerSupplyAsync {
        getResourceSections(facet)
    }

    override fun getOtherModulesResourceLists() = resourceExplorerSupplyAsync supplier@{
        val displayedModuleNames = mutableSetOf(facet.module.name)
        return@supplier findCompatibleFacets(facet.module.project).filter { facet ->
            // Don't include modules that are already being displayed.
            !displayedModuleNames.contains(facet.module.name)
        }.flatMap { facet ->
            getResourceSections(facet)
        }
    }

    override fun facetUpdated(newFacet: AndroidFacet) {
        facetUpdaterCallback?.invoke(newFacet)
    }

    private fun getResourceSections(forFacet: AndroidFacet): List<DesignSection> {
        val resourceType = currentDesignSystemType
        val resources = mutableListOf<DesignSection>()
        resources.add(
            DesignSection(
                resourceType,
                getModuleResources(forFacet, resourceType).map {
                    DesignAssetSet(it.name, it)
                }
            )
        )
        return resources
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