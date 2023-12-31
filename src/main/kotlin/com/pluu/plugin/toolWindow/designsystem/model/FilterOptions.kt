package com.pluu.plugin.toolWindow.designsystem.model

import kotlin.properties.Delegates

class FilterOptions internal constructor(
    private val refreshResourcesCallback: () -> Unit = {},
    private val searchStringChanged: (String) -> Unit = {},
    isShowSampleImage: Boolean,
) {
    /**
     * If true, the resources from the dependent modules will be shown.
     */
    var isShowSampleImage: Boolean by Delegates.observable(isShowSampleImage) { _, old, new ->
        if (new != old) refreshResourcesCallback()
    }

    /**
     * The search string to use to filter resources
     */
    var searchString: String by Delegates.observable("") { _, old, new ->
        if (new != old) searchStringChanged(new)
    }

    companion object {
        /** Helper function to instantiate [FilterOptions] with an initial state. */
        fun create(
            isShowResourcesChanged: () -> Unit,
            searchStringChanged: (String) -> Unit,
            initialParams: FilterOptionsParams
        ) = FilterOptions(
            isShowResourcesChanged,
            searchStringChanged,
            isShowSampleImage = initialParams.isShowSampleImageInitialValue,
        )
    }
}

/** Params to define the initial state of [FilterOptions]. */
data class FilterOptionsParams(
    val isShowSampleImageInitialValue: Boolean
)