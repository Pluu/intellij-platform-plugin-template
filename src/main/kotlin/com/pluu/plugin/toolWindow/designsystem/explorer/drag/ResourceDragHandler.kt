package com.pluu.plugin.toolWindow.designsystem.explorer.drag

import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
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
 *
 * @param importResourceDelegate Object to which [TransferHandler.importData] is delegated.
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
private class ResourceFilesTransferHandler(
    private val assetList: JList<DesignAssetSet>
): TransferHandler() {

    override fun canImport(support: TransferSupport): Boolean {
        if (support.sourceDropActions and COPY != COPY) return false
        return false
    }

    override fun importData(comp: JComponent?, t: Transferable?): Boolean {
        return t != null
    }

    override fun getSourceActions(c: JComponent?) = COPY_OR_MOVE

    override fun getDragImage() = createDragPreview(assetList)

    override fun createTransferable(c: JComponent?): Transferable {
        c?.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        return com.pluu.plugin.toolWindow.designsystem.model.createTransferable(assetList.selectedValue.asset)
    }

    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        source?.cursor = Cursor.getDefaultCursor()
    }
}

internal class ResourceDragHandlerImpl (
) : ResourceDragHandler {
    override fun registerSource(assetList: JList<DesignAssetSet>) {
        assetList.dragEnabled = true
        assetList.dropMode = DropMode.ON
        assetList.transferHandler = ResourceFilesTransferHandler(assetList)
    }
}

private fun createDragPreview(draggedAssets: JList<DesignAssetSet>): BufferedImage {
    val component = draggedAssets.cellRenderer.getListCellRendererComponent(
        draggedAssets,
        draggedAssets.selectedValue, //show the preview of the focused and selected item
        draggedAssets.selectedIndex,
        false,
        false
    )
    // The component having no parent to lay it out an set its size, we need to manually to it, otherwise
    // validate() won't be executed.

    // Drag시 나오는 이미지의 넓이은 JList의 넓이를 사용
    component.setSize(draggedAssets.width, component.preferredSize.height)
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