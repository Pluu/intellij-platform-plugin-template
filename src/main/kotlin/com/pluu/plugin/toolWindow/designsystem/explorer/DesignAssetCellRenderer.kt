package com.pluu.plugin.toolWindow.designsystem.explorer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/explorer/DesignAssetCellRenderer.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.ui.resourcemanager.rendering.AssetIconProvider
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.IconType
import com.pluu.plugin.toolWindow.designsystem.rendering.DesignAssetPreviewManager
import icons.PluuIcons
import java.awt.Component
import javax.swing.Icon
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
        val assetListView = list as AssetListView
        val assetView = assetListView.assetView
        val designSystemItem = value.asset

        if (assetListView.isGridMode) {
            assetView.thumbnail = designSystemIcon(designSystemItem.type.icon)
            assetView.withChessboard = false
        } else {
            if (assetView.sampleImageSize.isVisible()) {
                val thumbnailSize = assetView.thumbnailSize
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

    private fun designSystemIcon(assetSet: IconType): Icon  = when (assetSet) {
        IconType.Text -> PluuIcons.iconText
        IconType.Control -> PluuIcons.iconSlider
        IconType.Toast -> PluuIcons.iconToast
        IconType.Button -> PluuIcons.iconButton
        IconType.Etc -> PluuIcons.iconNone
    }
}