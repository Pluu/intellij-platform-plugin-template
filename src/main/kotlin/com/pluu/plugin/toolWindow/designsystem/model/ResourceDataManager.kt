package com.pluu.plugin.toolWindow.designsystem.model

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

fun createTransferable(asset: DesignSystemItem): Transferable {
    val resourceUrl = asset.file

    return object : Transferable {
        override fun getTransferData(flavor: DataFlavor?): Any {
            return resourceUrl
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = false

        override fun getTransferDataFlavors(): Array<DataFlavor> = emptyArray()

    }
}
