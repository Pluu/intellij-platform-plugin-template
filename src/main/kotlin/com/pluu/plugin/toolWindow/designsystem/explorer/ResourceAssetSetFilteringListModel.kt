package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.resources.ResourceType
import com.android.tools.idea.ui.resourcemanager.model.ResourceAssetSet
import com.intellij.openapi.util.Condition
import com.intellij.ui.CollectionListModel
import com.intellij.ui.speedSearch.FilteringListModel
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet

/**
 * [FilteringListModel] for [ResourceAssetSet] matching name and [ResourceType.STRING] values.
 */
class ResourceAssetSetFilteringListModel(
    collectionListModel: CollectionListModel<DesignAssetSet>,
    private val filter: Condition<String>
) : FilteringListModel<DesignAssetSet>(collectionListModel) {
    init {
        setFilter(::isMatch)
    }

    private fun isMatch(assetSet: DesignAssetSet): Boolean {
        if (filter.value(assetSet.name)) {
            return true
        }
        return false
    }
}
