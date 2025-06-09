package com.pluu.plugin.toolWindow.designsystem.explorer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/explorer/DesignAssetCellRenderer.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.ui.resourcemanager.rendering.AssetIconProvider
import com.intellij.util.IconUtil
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import java.awt.Component
import java.awt.Dimension
import java.awt.Image
import javax.swing.ImageIcon
import javax.swing.JList
import javax.swing.ListCellRenderer
import kotlin.math.min

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
        val assetListView = list as AssetListView
        val assetView = assetListView.assetView
        val designSystemItem = value.asset
        val thumbnailSize = assetView.thumbnailSize

        if (assetListView.isGridMode) {
            assetView.thumbnail = if (thumbnailSize.width > 0 && thumbnailSize.height > 0) {
                val iconSize = Dimension(designSystemItem.type.icon.iconWidth, designSystemItem.type.icon.iconHeight)
                val scale = getScale(
                    Dimension((thumbnailSize.width ).toInt(), (thumbnailSize.height).toInt()),
                    iconSize
                )
                val image = IconUtil.toBufferedImage(designSystemItem.type.icon)
                    .getScaledInstance(
                        (iconSize.width * scale).toInt(),
                        (iconSize.height * scale).toInt(),
                        Image.SCALE_SMOOTH
                    )
                ImageIcon(image)
            } else {
                null
            }
            assetView.withChessboard = false
        } else {
            if (assetView.isVisibleThumbnail) {
                val iconProvider = assetPreviewManager.getPreviewProvider()
                val icon = iconProvider.getIcon(
                    designSystemItem,
                    thumbnailSize.width,
                    thumbnailSize.height,
                    list,
                    { list.getCellBounds(index, index)?.let(list::repaint) },
                    { index in list.firstVisibleIndex..list.lastVisibleIndex })
                assetView.thumbnail = icon
                assetView.withChessboard = iconProvider.supportsTransparency
            }
        }
        assetView.selected = isSelected
        assetView.focused = cellHasFocus

        assetView.componentName = designSystemItem.name
        assetView.applicableFileType = designSystemItem.applicableFileType.takeIf { it.isSelectable() }
        assetView.aliasName = designSystemItem.aliasNames?.joinToString(", ") ?: "-"
        assetView.typeName = designSystemItem.type.name.takeIf { isVisibleComponentName }.orEmpty()
        return assetView
    }

    /**
     * Get the scaling factor from [source] to [target].
     */
    private fun getScale(target: Dimension, source: Dimension): Double {
        val xScale = target.width / source.getWidth()
        val yScale = target.height / source.getHeight()
        return min(xScale, yScale)
    }
}