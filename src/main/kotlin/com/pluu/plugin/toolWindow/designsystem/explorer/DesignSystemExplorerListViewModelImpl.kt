package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.ide.common.resources.ResourceResolver
import com.android.tools.idea.ui.resourcemanager.model.ResourceSection
import com.android.tools.idea.ui.resourcemanager.model.TypeFilter
import com.android.tools.idea.ui.resourcemanager.rendering.AssetPreviewManager
import com.android.tools.idea.ui.resourcemanager.rendering.AssetPreviewManagerImpl
import com.android.tools.idea.ui.resourcemanager.rendering.ImageCache
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.util.concurrency.AppExecutorUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.findCompatibleFacets
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.model.getDependentModuleResources
import com.pluu.plugin.toolWindow.designsystem.model.getModuleResources
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import kotlin.properties.Delegates

class DesignSystemExplorerListViewModelImpl(
    override val facet: AndroidFacet,
    private val contextFile: VirtualFile?,
    private val resourceResolver: ResourceResolver,
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

    override val assetPreviewManager: AssetPreviewManager =
        AssetPreviewManagerImpl(facet, listViewImageCache, resourceResolver, contextFile)

    override fun getCurrentModuleResourceLists() = resourceExplorerSupplyAsync {
        getResourceSections(
            facet,
            showModuleDependencies = filterOptions.isShowModuleDependencies
        )
    }

    override fun getOtherModulesResourceLists() = resourceExplorerSupplyAsync supplier@{
        val displayedModuleNames = mutableSetOf(facet.module.name)
        return@supplier findCompatibleFacets(facet.module.project).filter { facet ->
            // Don't include modules that are already being displayed.
            !displayedModuleNames.contains(facet.module.name)
        }.flatMap { facet ->
            getResourceSections(
                facet,
                showModuleDependencies = false
            )
        }
    }

    override fun facetUpdated(newFacet: AndroidFacet) {
        facetUpdaterCallback?.invoke(newFacet)
    }

    private fun getResourceSections(
        forFacet: AndroidFacet,
        showModuleDependencies: Boolean = true,
        typeFilters: List<TypeFilter> = emptyList()
    ): List<ResourceSection> {
        val resourceType = currentDesignSystemType
        val resources = mutableListOf<ResourceSection>()
        resources.add(getModuleResources(forFacet, resourceType))
        if (showModuleDependencies) {
            resources.addAll(getDependentModuleResources(forFacet, resourceType, typeFilters))
        }
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