package com.pluu.plugin.toolWindow.designsystem.model

import kotlin.properties.Delegates

class FilterOptions internal constructor(
    private val refreshResourcesCallback: () -> Unit = {},
    private val searchStringChanged: (String) -> Unit = {},
    moduleDependenciesInitialValue: Boolean,
) {
    /**
     * If true, the resources from the dependent modules will be shown.
     */
    var isShowModuleDependencies: Boolean
            by Delegates.observable(moduleDependenciesInitialValue) { _, old, new -> if (new != old) refreshResourcesCallback() }

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
            searchStringChanged: (String) -> Unit
        ) = FilterOptions(
            isShowResourcesChanged,
            searchStringChanged,
            moduleDependenciesInitialValue = true,
        )
    }
}