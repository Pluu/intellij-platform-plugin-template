package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.resources.ResourceType
import com.android.tools.idea.ui.resourcemanager.model.ResourceAssetSet
import com.intellij.openapi.util.Condition
import com.intellij.ui.CollectionListModel
import com.intellij.ui.speedSearch.FilteringListModel

/**
 * [FilteringListModel] for [ResourceAssetSet] matching name and [ResourceType.STRING] values.
 */
class ResourceAssetSetFilteringListModel(
    collectionListModel: CollectionListModel<ResourceAssetSet>,
    private val filter: Condition<String>
) : FilteringListModel<ResourceAssetSet>(collectionListModel) {
    init {
        setFilter(::isMatch)
    }

    private fun isMatch(assetSet: ResourceAssetSet): Boolean {
        if (filter.value(assetSet.name)) {
            return true
        }
        for (asset in assetSet.assets) {
            if (asset.type == ResourceType.STRING) {
                val value = asset.resourceItem.resourceValue?.value
                if (value?.let { filter.value(it) } == true) {
                    return true
                }
            }
        }
        return false
    }
}
