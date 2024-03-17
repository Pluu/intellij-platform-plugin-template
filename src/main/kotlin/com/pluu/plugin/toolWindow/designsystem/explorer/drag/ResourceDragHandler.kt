package com.pluu.plugin.toolWindow.designsystem.explorer.drag

import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.createCopyTransferable
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

/**
 * Create a new [ResourceDragHandler].
 *
 * Handles dragging out [DesignAssetSet]s in a list.
 *
 * E.g: Drag a Drawable [DesignAssetSet] into the LayoutEditor.
 */
fun resourceDragHandler() = if (GraphicsEnvironment.isHeadless()) {
    HeadlessDragHandler()
} else {
    ResourceDragHandlerImpl()
}

interface ResourceDragHandler {
    fun registerSource(assetList: JList<DesignAssetSet>)
}

/**
 * DragHandler in headless mode
 */
class HeadlessDragHandler internal constructor() : ResourceDragHandler {
    override fun registerSource(assetList: JList<DesignAssetSet>) {
        // Do Nothing
    }
}

/**
 * Handles the transfers of the assets when they gets dragged
 */
class ResourceFilesTransferHandler(
    private val assetList: JList<DesignAssetSet>
) : TransferHandler() {

    override fun getSourceActions(c: JComponent?) = COPY_OR_MOVE

    override fun getDragImage() = createDragPreview(assetList)

    override fun createTransferable(c: JComponent?): Transferable {
        c?.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        return createCopyTransferable(assetList.selectedValue.asset)
    }
}

internal class ResourceDragHandlerImpl : ResourceDragHandler {
    override fun registerSource(assetList: JList<DesignAssetSet>) {
        assetList.dragEnabled = true
        assetList.dropMode = DropMode.ON
        assetList.transferHandler = ResourceFilesTransferHandler(assetList)
    }
}

private fun createDragPreview(draggedAssets: JList<DesignAssetSet>): BufferedImage {
    val component = draggedAssets.cellRenderer.getListCellRendererComponent(
        draggedAssets,
        draggedAssets.selectedValue, // 프리뷰로 노출할 아이템
        draggedAssets.selectedIndex,
        false,
        false
    )

    // Drag시 Preview 크기 : JList의 넓이 x Component의 높이
    component.setSize(draggedAssets.preferredSize.width, component.preferredSize.height)
    component.validate()

    // Dimensions for BufferedImage are pre-scaled.
    @Suppress("UndesirableClassUsage")
    val image = BufferedImage(draggedAssets.width, component.height, BufferedImage.TYPE_INT_ARGB)
    with(image.createGraphics()) {
        color = draggedAssets.background
        fillRect(0, 0, draggedAssets.width, component.height)
        component.paint(this)
        dispose()
    }
    return image
}