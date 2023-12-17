package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.SdkConstants
import com.android.ide.common.resources.ResourceResolver
import com.android.ide.common.resources.configuration.FolderConfiguration
import com.android.tools.configurations.Configuration
import com.android.tools.idea.configurations.ConfigurationManager
import com.android.tools.idea.ui.resourcemanager.rendering.ImageCache
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.update.MergingUpdateQueue
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import org.jetbrains.android.dom.manifest.getPrimaryManifestXml
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.sdk.StudioEmbeddedRenderTarget
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
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
        {
            updateFilterParamsInModelState()
            refreshListModel()
        },
        {
            updateListModelSpeedSearch(it)
        }
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
        val configurationFuture = getConfiguration(facet, contextFileForConfiguration)
        return getResourceResolver(facet, configurationFuture)
            .thenApplyAsync(
                { resourceResolver ->
                    DesignSystemExplorerListViewModelImpl(
                        facet,
                        contextFileForConfiguration,
                        resourceResolver,
                        listViewImageCache,
                        filterOptions,
                        supportedTypes[supportTypeIndex]
                    ).also {
                        listViewModel = it
                        it.facetUpdaterCallback = { newFacet -> this@DesignSystemExplorerViewModel.facet = newFacet }
                        updateListModelIfNeeded()
                    }
                }, EdtExecutorService.getInstance()
            )
    }

    override fun dispose() {

    }

    private fun updateFilterParamsInModelState() {
//        modelState.filterParams = FilterOptionsParams(
//            moduleDependenciesInitialValue = filterOptions.isShowModuleDependencies,
//            librariesInitialValue = filterOptions.isShowLibraries,
//            androidResourcesInitialValue = filterOptions.isShowFramework,
//            themeAttributesInitialValue = filterOptions.isShowThemeAttributes,
//            showSampleData = filterOptions.isShowSampleData
//        )
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

private fun getConfiguration(facet: AndroidFacet, contextFile: VirtualFile? = null): CompletableFuture<Configuration?> =
    CompletableFuture.supplyAsync(Supplier {
        val configManager = ConfigurationManager.getOrCreateInstance(facet.module)
        var configuration: Configuration? = null
        contextFile?.let {
            configuration = configManager.getConfiguration(contextFile)
        }
        if (configuration == null) {
            runReadAction { facet.getPrimaryManifestXml() }?.let { manifestFile ->
                configuration = configManager.getConfiguration(manifestFile.virtualFile)
            }
        }
        return@Supplier configuration
    }, AppExecutorUtil.getAppExecutorService())

/**
 * Initializes the [ResourceResolver] in a background thread.
 *
 * @param facet The current [AndroidFacet], used to fallback to get a [ResourceResolver] in case [configurationFuture] cannot provide a
 * [Configuration].
 * @param configurationFuture A [CompletableFuture] that may return a [Configuration], if it does, it'll get the [ResourceResolver] from it.
 */
private fun getResourceResolver(
    facet: AndroidFacet,
    configurationFuture: CompletableFuture<Configuration?>
): CompletableFuture<ResourceResolver> {
    return configurationFuture.thenApplyAsync({ configuration ->
        configuration?.let { return@thenApplyAsync it.resourceResolver }
        val configurationManager = ConfigurationManager.getOrCreateInstance(facet.module)
        val theme = SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX + "Theme.Material.Light"
        val target =
            configurationManager.highestApiTarget?.let { StudioEmbeddedRenderTarget.getCompatibilityTarget(it) }
        return@thenApplyAsync configurationManager.resolverCache.getResourceResolver(
            target,
            theme,
            FolderConfiguration.createDefault()
        )
    }, AppExecutorUtil.getAppExecutorService())
}