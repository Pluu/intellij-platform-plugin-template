package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.model.ResourceAssetSet
import com.android.tools.idea.ui.resourcemanager.rendering.AssetIconProvider
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.rendering.DefaultIconProvider
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetIconProvider
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * [ListCellRenderer] to render [ResourceAssetSet] using an [AssetIconProvider]
 * returned by the [assetPreviewManager].
 */
class DesignAssetCellRenderer(
    private val assetPreviewManager: DesignAssetPreviewManager
) : ListCellRenderer<DesignAssetSet> {

    val label = JLabel().apply { horizontalAlignment = JLabel.CENTER }

    override fun getListCellRendererComponent(
        list: JList<out DesignAssetSet>,
        value: DesignAssetSet,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val assetView = (list as AssetListView).assetView
        val thumbnailSize = assetView.thumbnailSize
        val assetToRender = value.asset

        val iconProvider: DesignAssetIconProvider = assetPreviewManager.getPreviewProvider(assetToRender.type)
        label.icon = iconProvider.getIcon(assetToRender,
            thumbnailSize.width,
            thumbnailSize.height,
            list,
            { list.getCellBounds(index, index)?.let(list::repaint) },
            { index in list.firstVisibleIndex..list.lastVisibleIndex })
        // DefaultIconProvider provides an empty icon, to avoid comparison, we just set the thumbnail to null.
        assetView.thumbnail = if (iconProvider is DefaultIconProvider) null else label
        assetView.withChessboard = iconProvider.supportsTransparency
        assetView.selected = isSelected
        assetView.focused = cellHasFocus

        assetView.title = assetToRender.name
        assetView.subtitle = assetToRender.type.name
//        assetView.metadata = metadata
//        if (RESOURCE_DEBUG) {
//            assetView.issueLevel = IssueLevel.ERROR
//            assetView.isNew = true
//        }
        return assetView
    }
}