package com.pluu.plugin.toolWindow.designsystem.model

import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import kotlin.properties.Delegates

class FilterOptions internal constructor(
    private val refreshResourcesCallback: () -> Unit = {},
    private val searchStringChanged: (String) -> Unit = {},
    sampleImageSize: FilterImageSize,
) {
    /**
     * Model for filters by type. Eg: To filter Drawables by 'vector-drawables' files.
     *
     * @see TypeFilter
     */
    val typeFiltersModel = TypeFiltersModel().apply { valueChangedCallback = refreshResourcesCallback }

    /**
     * If true, the resources from the dependent modules will be shown.
     */
    var sampleImageSize: FilterImageSize by Delegates.observable(sampleImageSize) { _, old, new ->
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
            sampleImageSize = initialParams.sampleImageSizeInitialValue,
        )
    }
}

/** Params to define the initial state of [FilterOptions]. */
data class FilterOptionsParams(
    val sampleImageSizeInitialValue: FilterImageSize
)

/**
 * Describes the filter options available for a particular [DesignSystemType]. The state of each filter option can be set as disabled or enabled
 * and it will be preserved for the duration of the instantiated model. Every filter option is initialized to 'false/disabled' by default.
 *
 * Set the callback function [valueChangedCallback] to react to changes made to the model.
 */
class TypeFiltersModel {
    /** Callback function, called when a filter options changes value. */
    var valueChangedCallback: () -> Unit = {}

    private val resourceTypeToFilter = getTypeFiltersMap().toMutableMap()

    /**
     * For the given [type], returns a list of the supported [TypeFilter]s.
     */
    fun getSupportedImageSize() = FilterImageSize.values()

    /**
     * The current state for the given [TypeFilter], returns false if it doesn't exist for the given [DesignSystemType].
     */
    fun isEnabled(type: FilterType, filter: FilterOption): Boolean =
        resourceTypeToFilter[type]?.let {
            it.first == filter
        } ?: false

    /**
     * Set the state for a given [TypeFilter] if it exists for the given [DesignSystemType]. If it results in value change Eg: false ->
     * true. Triggers the [valueChangedCallback] function.
     */
    fun setEnabled(type: FilterType, filter: FilterOption, value: Boolean) {
        resourceTypeToFilter[type] = filter to value
        valueChangedCallback()
    }

    /**
     * Sets all the [TypeFilter]s under the given [DesignSystemType] to false (ie: disabled).
     */
    fun clearAll(type: DesignSystemType) {

    }
}

enum class FilterType {
    IMAGE_SIZE
}

interface FilterOption

enum class FilterImageSize : FilterOption {
    None, S, M, L;

    fun isVisible() = this != None
}

private fun getTypeFiltersMap(): Map<FilterType, Pair<FilterOption, Boolean>> {
    return mapOf(
        FilterType.IMAGE_SIZE to (FilterImageSize.M to true)
    )
}