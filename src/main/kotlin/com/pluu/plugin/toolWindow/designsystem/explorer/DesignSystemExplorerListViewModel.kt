package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.ui.speedSearch.SpeedSearch
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSection
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture

interface DesignSystemExplorerListViewModel {
    /**
     * The reason why [updateUiCallback] is being called.
     */
    enum class UpdateUiReason {
        /**
         * Resource information has changed, caused by adding/editing/removing resources or from changing Module, so the list of resources has
         * to be refreshed.
         */
        DESIGN_SYSTEM_CHANGED,
        /**
         * Resource Type of the model has changed, after this, the view should reset back to showing the list of resources if it was in any
         * other state, like showing resource configurations.
         */
        DESIGN_SYSTEM_TYPE_CHANGED,
        /**
         * The Image Cache has changed, it might be necessary to repaint.
         */
        IMAGE_CACHE_CHANGED
    }

    var updateUiCallback: ((UpdateUiReason) -> Unit)?

    var facetUpdaterCallback: ((facet: AndroidFacet) -> Unit)?

    /**
     * The current [DesignSystemType] of resources being fetched.
     */
    var currentDesignSystemType: DesignSystemType

    val selectedTabName: String get() = ""

    val assetPreviewManager: DesignAssetPreviewManager

    val facet: AndroidFacet

    val speedSearch: SpeedSearch

    val filterOptions: FilterOptions

    /**
     * Clears the cached image for all resources being currently displayed for the [currentDesignSystemType].
     *
     * This considers the fields in [FilterOptions], except for [FilterOptions.searchString].
     */
    fun clearCacheForCurrentResources()

    /**
     * Clears the cached image for the given [DesignSystemItem].
     *
     * Clearing the cached image will indirectly result in a new image being rendered and cached.
     */
    fun clearImageCache(asset: DesignSystemItem)

    /**
     * Returns a list of [DesignSection] with one section per namespace, the first section being the
     * one containing the resource of the current module.
     */
    fun getDesignSections(): CompletableFuture<List<DesignSection>>

    /**
     * Triggers an [AndroidFacet] change through [facetUpdaterCallback].
     *
     * Eg: Searching for resources matching 'ic' and clicking the LinkLabel to switch to module Foo that contains resources matching the
     * filter. All components of the ResourceExplorer should update to module Foo.
     */
    fun facetUpdated(newFacet: AndroidFacet)
}