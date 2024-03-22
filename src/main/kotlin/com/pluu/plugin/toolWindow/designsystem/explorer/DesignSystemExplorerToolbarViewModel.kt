package com.pluu.plugin.toolWindow.designsystem.explorer

///////////////////////////////////////////////////////////////////////////
// Origin ; https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/explorer/ResourceExplorerToolbarViewModel.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.FilterImageSize
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.model.TypeFiltersModel
import org.jetbrains.android.facet.AndroidFacet
import kotlin.properties.Delegates

class DesignSystemExplorerToolbarViewModel(
    facet: AndroidFacet,
    initialDesignSystemType: DesignSystemType?,
    private val filterOptions: FilterOptions
) : DataProvider {

    /**
     * Callback added by the view to be called when data of this
     * view model changes.
     */
    var updateUICallback = {}

    /** View callback whenever the resources lists needs to be repopulated. */
    var populateResourcesCallback: (() -> Unit) = {}

    /** Called when a new facet is selected. */
    var facetUpdaterCallback: (AndroidFacet) -> Unit = {}

    /** Callback for when a request to refresh resources previews is made. */
    var refreshResourcesPreviewsCallback: () -> Unit = {}

    var requestSearch = {}

    var resourceType: DesignSystemType? by Delegates.observable(initialDesignSystemType) { _, oldValue, newValue ->
        if (newValue != oldValue) {
            updateUICallback()
        }
    }

    var facet: AndroidFacet = facet
        set(newFacet) {
            if (field != newFacet) {
                field = newFacet
                updateUICallback()
            }
        }

    var searchString: String by Delegates.observable("") { _, old, new ->
        if (new != old) {
            filterOptions.searchString = new
        }
    }

    var sampleImageSize: FilterImageSize
        get() = filterOptions.sampleImageSize
        set(value) {
            filterOptions.sampleImageSize = value
        }

    var typeFiltersModel: TypeFiltersModel = filterOptions.typeFiltersModel

    override fun getData(dataId: String): Any? = when (dataId) {
        CommonDataKeys.PROJECT.name -> facet.module.project
        PlatformCoreDataKeys.MODULE.name -> facet.module
        LangDataKeys.IDE_VIEW.name -> this
        else -> null
    }
}