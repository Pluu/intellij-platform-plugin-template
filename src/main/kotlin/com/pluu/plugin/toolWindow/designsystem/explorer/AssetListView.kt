package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.model.ResourceAssetSet
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.intellij.ui.speedSearch.FilteringListModel
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.widget.AssetView
import com.pluu.plugin.toolWindow.designsystem.widget.RowAssetView
import com.pluu.plugin.toolWindow.designsystem.widget.SingleAssetCard
import java.awt.event.MouseEvent
import javax.swing.JList
import kotlin.properties.Delegates

private val DEFAULT_PREVIEW_SIZE = JBUI.scale(50)

private const val DEFAULT_GRID_MODE = false

/**
 * [JList] to display [ResourceAssetSet] and handle switching
 * between grid and list mode.
 */
class AssetListView(
    assets: List<ResourceAssetSet>,
    speedSearch: SpeedSearch? = null
) : JBList<ResourceAssetSet>() {

    var isGridMode: Boolean by Delegates.observable(DEFAULT_GRID_MODE) { _, _, isGridMode ->
        if (isGridMode) {
            layoutOrientation = JList.HORIZONTAL_WRAP
            assetView = SingleAssetCard()
            setExpandableItemsEnabled(false)
        } else {
            layoutOrientation = JList.VERTICAL
            assetView = RowAssetView()
            setExpandableItemsEnabled(true)
        }
        updateCellSize()
    }

    lateinit var assetView: AssetView
        private set


    /**
     * Width of the [AssetView] thumbnail container
     */
    var thumbnailWidth: Int by Delegates.observable(DEFAULT_PREVIEW_SIZE) { _, oldWidth, newWidth ->
        if (oldWidth != newWidth) {
            updateCellSize()
        }
    }

    private val filteringListModel: FilteringListModel<ResourceAssetSet>?

    init {
        isOpaque = false
        visibleRowCount = 0
        isGridMode = DEFAULT_GRID_MODE
        val collectionListModel = CollectionListModel(assets)
        if (speedSearch != null) {
            speedSearch.setEnabled(true)
            filteringListModel = ResourceAssetSetFilteringListModel(collectionListModel, speedSearch::shouldBeShowing)
            model = filteringListModel
        } else {
            filteringListModel = null
            model = collectionListModel
        }
    }

    /**
     * If a [SpeedSearch] was provided in constructor, filters the list items using the [SpeedSearch.getFilter].
     */
    fun refilter() {
        filteringListModel?.refilter()
    }

    fun getFilteredSize(): Int? {
        return filteringListModel?.size
    }

    private fun updateCellSize() {
        assetView.viewWidth = thumbnailWidth
        fixedCellWidth = assetView.preferredSize.width
        fixedCellHeight = assetView.preferredSize.height
        revalidate()
        repaint()
    }

    // The default implementation will will generate the tooltip from the
    // list renderer, which is quite expensive in our case, and not needed.
    override fun getToolTipText(event: MouseEvent?): String? = null
}