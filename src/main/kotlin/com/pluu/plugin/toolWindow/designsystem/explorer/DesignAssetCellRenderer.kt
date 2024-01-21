package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.rendering.AssetIconProvider
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetIconProvider
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * [ListCellRenderer] to render [DesignAssetSet] using an [AssetIconProvider]
 * returned by the [assetPreviewManager].
 */
class DesignAssetCellRenderer(
    private val assetPreviewManager: DesignAssetPreviewManager
) : ListCellRenderer<DesignAssetSet> {

    private val label = JLabel().apply { horizontalAlignment = JLabel.CENTER }

    override fun getListCellRendererComponent(
        list: JList<out DesignAssetSet>,
        value: DesignAssetSet,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val assetView = (list as AssetListView).assetView
        val designSystemItem = value.asset

        if (assetView.sampleImageSize.isVisible()) {
            val thumbnailSize = assetView.thumbnailSize
            val iconProvider: DesignAssetIconProvider = assetPreviewManager.getPreviewProvider(designSystemItem.type)
            label.icon = iconProvider.getIcon(designSystemItem,
                thumbnailSize.width,
                thumbnailSize.height,
                list,
                { list.getCellBounds(index, index)?.let(list::repaint) },
                { index in list.firstVisibleIndex..list.lastVisibleIndex })
            assetView.thumbnail = label
            assetView.withChessboard = iconProvider.supportsTransparency
        }
        assetView.selected = isSelected
        assetView.focused = cellHasFocus

        assetView.componentName = designSystemItem.name
        assetView.aliasName = designSystemItem.aliasNames?.joinToString(", ") ?: "-"
        assetView.applicableFileType = designSystemItem.applicableFileType.takeIf { it.isSelectable() }
        return assetView
    }
}