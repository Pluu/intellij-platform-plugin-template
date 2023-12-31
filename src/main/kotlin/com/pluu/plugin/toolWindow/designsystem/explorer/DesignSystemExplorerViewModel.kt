package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.update.MergingUpdateQueue
import com.pluu.plugin.toolWindow.designsystem.DESIGN_RES_MANAGER_PREF_KEY
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptionsParams
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCache
import org.jetbrains.android.facet.AndroidFacet
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates

internal class DesignSystemExplorerViewModel(
    defaultFacet: AndroidFacet,
    private var contextFileForConfiguration: VirtualFile?,
    var supportedTypes: Array<DesignSystemType>,
    private val modelState: ViewModelState,
    private val selectAssetAction: ((asset: DesignSystemItem) -> Unit)? = null,
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
        { updateListModelSpeedSearch(it) },
        modelState.filterParams
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

    /** View callback for when the DesignSystemType has changed. */
    var updateSupportTypeTabCallback: (() -> Unit) = {}

    /** View callback whenever the resources lists needs to be repopulated. */
    var populateResourcesCallback: (() -> Unit) = {}

    /** Callback called when the [AndroidFacet] has changed. */
    var facetUpdaterCallback: ((facet: AndroidFacet) -> Unit) = {}

    /** Callback called when the current [DesignSystemType] has changed. */
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
                modelState.selectedResourceType = supportedTypes[value]
                updateListModelDesignSystemType(supportedTypes[value])
                designSystemTypeUpdaterCallback(supportedTypes[value])
                updateSupportTypeTabCallback()
            }
        }

    fun refreshPreviews() {
        listViewModel?.clearCacheForCurrentResources()
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
                supportedTypes[supportTypeIndex],
                selectAssetAction
            ).also {
                listViewModel = it
                it.facetUpdaterCallback = { newFacet -> this@DesignSystemExplorerViewModel.facet = newFacet }
                updateListModelIfNeeded()
            }
        }, EdtExecutorService.getInstance())
    }

    override fun dispose() {

    }

    private fun updateFilterParamsInModelState() {
        modelState.filterParams = FilterOptionsParams(
            isShowSampleImageInitialValue = filterOptions.isShowSampleImage
        )
    }

    //region ListModel update functions
    fun refreshListModel() {
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
                DesignSystemType.values(),
                ViewModelState(
                    FilterOptionsParams(
                        isShowSampleImageInitialValue = true,
                    ),
                    DesignSystemType.values()[0],
                    ViewModelStateSaveParams(facet.module.project, DESIGN_RES_MANAGER_PREF_KEY)
                )
            )
        }
    }
}

private const val FILTER_PARAMS_KEY = "FilterParams"
private const val SHOW_SAMPLE_IMAGE = "ShowSampleImage"
private const val DESIGN_SYSTEM_TYPE_KEY = "DesignSystemType"

/**
 * Class that holds the initial state of [DesignSystemExplorerViewModel].
 *
 * If [saveParams] is not-null, it will save the latest changes of this state.
 */
internal class ViewModelState(
    filterParams: FilterOptionsParams,
    selectedResourceType: DesignSystemType,
    private val saveParams: ViewModelStateSaveParams? = null
) {

    private val defaultFilterParams: FilterOptionsParams = kotlin.run {
        return@run if (saveParams != null) {
            val filterKey = "${saveParams.preferencesKey}.$FILTER_PARAMS_KEY"
            val propertiesComponent = PropertiesComponent.getInstance(saveParams.project)
            val showSampleImage = propertiesComponent.getBoolean("$filterKey.$SHOW_SAMPLE_IMAGE")
            FilterOptionsParams(
                isShowSampleImageInitialValue = showSampleImage
            )
        } else {
            filterParams
        }
    }

    private val defaultSelectedResourceType: DesignSystemType = kotlin.run {
        return@run if (saveParams != null) {
            PropertiesComponent.getInstance(saveParams.project)
                .getValue("${saveParams.preferencesKey}.$DESIGN_SYSTEM_TYPE_KEY")?.let {
                    DesignSystemType.valueOf(it)
            } ?: selectedResourceType
        } else {
            selectedResourceType
        }
    }

    var filterParams: FilterOptionsParams by Delegates.observable(defaultFilterParams) { _, _, newValue ->
        saveParams?.let {
            val filterKey = "${saveParams.preferencesKey}.$FILTER_PARAMS_KEY"
            val propertiesComponent = PropertiesComponent.getInstance(saveParams.project)
            propertiesComponent.setValue("$filterKey.$SHOW_SAMPLE_IMAGE", newValue.isShowSampleImageInitialValue)
        }
    }

    var selectedResourceType: DesignSystemType by Delegates.observable(defaultSelectedResourceType) { _, _, newValue ->
        saveParams?.let {
            PropertiesComponent.getInstance(saveParams.project)
                .setValue("${saveParams.preferencesKey}.$DESIGN_SYSTEM_TYPE_KEY", newValue.name)
        }
    }
}

/**
 * Necessary parameters to save the state of [ViewModelState] on a project-level basis.
 */
internal class ViewModelStateSaveParams(
    val project: Project,
    val preferencesKey: String
)