package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.rendering.AssetIconProvider
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import java.awt.Component
import java.awt.Dimension
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * [ListCellRenderer] to render [DesignAssetSet] using an [AssetIconProvider]
 * returned by the [assetPreviewManager].
 */
class DesignAssetCellRenderer(
    private val assetPreviewManager: DesignAssetPreviewManager,
    private val isVisibleComponentName: Boolean
) : ListCellRenderer<DesignAssetSet> {

    override fun getListCellRendererComponent(
        list: JList<out DesignAssetSet>,
        value: DesignAssetSet,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val assetView = (list as DesignSystemExplorerListView).assetView
        val designSystemItem = value.asset

        if (assetView.sampleImageSize.isVisible()) {
            val thumbnailSize = assetView.thumbnailSize
            val iconProvider = assetPreviewManager.getPreviewProvider()
            val icon = iconProvider.getIcon(designSystemItem,
                thumbnailSize.width,
                thumbnailSize.height,
                list,
                { list.getCellBounds(index, index)?.let(list::repaint) },
                { index in list.firstVisibleIndex..list.lastVisibleIndex })
            assetView.thumbnail = icon
            assetView.withChessboard = iconProvider.supportsTransparency
        }
        assetView.selected = isSelected
        assetView.focused = cellHasFocus

        assetView.componentName = designSystemItem.name
        assetView.applicableFileType = designSystemItem.applicableFileType.takeIf { it.isSelectable() }
        assetView.aliasName = designSystemItem.aliasName ?: "-"
        assetView.typeName = designSystemItem.type.name.takeIf { isVisibleComponentName }.orEmpty()
        assetView.preferredSize = Dimension(Integer.MAX_VALUE, assetView.preferredSize.height)
        return assetView
    }
}