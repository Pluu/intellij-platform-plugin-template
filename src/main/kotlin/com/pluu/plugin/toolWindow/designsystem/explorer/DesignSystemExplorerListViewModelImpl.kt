package com.pluu.plugin.toolWindow.designsystem.explorer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/explorer/ResourceExplorerListViewModelImpl.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.codeInsight.navigation.openFileWithPsiElement
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSection
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemTab
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.model.ResourceDataManager
import com.pluu.plugin.toolWindow.designsystem.provider.DesignSystemManager
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManagerImpl
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCache
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import kotlin.properties.Delegates

class DesignSystemExplorerListViewModelImpl(
    override val facet: AndroidFacet,
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

    private val dataManager = ResourceDataManager(facet)

    override val selectedTabName: String get() = currentTab.name

    override val speedSearch = SpeedSearch(true).apply {
        if (filterOptions.searchString.isNotEmpty()) {
            updatePattern(filterOptions.searchString)
        }
    }

    override val assetPreviewManager: DesignAssetPreviewManager =
        DesignAssetPreviewManagerImpl(listViewImageCache)

    override fun clearCacheForCurrentResources() {
        getCurrentModuleResourceLists().whenCompleteAsync({ lists, throwable ->
            if (throwable == null) {
                lists.flatMap { section ->
                    section.assetSets.map { it.asset }
                }.forEach(::clearImageCache)
                updateUiCallback?.invoke(UpdateUiReason.IMAGE_CACHE_CHANGED)
            }
        }, EdtExecutorService.getInstance())
    }

    override fun clearImageCache(asset: DesignSystemItem) {
        listViewImageCache.clear(asset)
    }

    override fun getCurrentModuleResourceLists(): CompletableFuture<List<DesignSection>> = resourceExplorerSupplyAsync {
        getResourceSections(facet)
    }

    private fun getResourceSections(forFacet: AndroidFacet): List<DesignSection> {
        val categoryType = currentTab.filterType
        val designSections = mutableListOf<DesignSection>()
        designSections.add(
            DesignSection(
                name = currentTab.name,
                assetSets = DesignSystemManager.getDesignSystemResources(forFacet.module.project, categoryType)
                    .map {
                        DesignAssetSet(it.name, it)
                    },
                isVisibleTypeName = currentTab.filterType == null
            )
        )
        return designSections
    }

    override fun uiDataSnapshot(sink: DataSink, selectedAssets: List<DesignSystemItem>) {
        dataManager.uiDataSnapshot(sink, selectedAssets)
    }

    override val doSelectAssetAction: (asset: DesignSystemItem) -> Unit = selectAssetAction ?: { asset ->
        val psiElement = dataManager.findPsiElement(asset)
        psiElement?.let {
            openFileWithPsiElement(it, searchForOpen = true, requestFocus = true)
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