package com.pluu.plugin.toolWindow.designsystem.model

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

/**
 * [DataFlavor] for [DesignSystemItem]
 */
@JvmField
val DESIGN_SYSTEM_URL_FLAVOR = DataFlavor(DesignSystemItem::class.java, "DesignSystem File Url")

private val SUPPORTED_DATA_FLAVORS = arrayOf(DESIGN_SYSTEM_URL_FLAVOR, DataFlavor.stringFlavor)

fun createCopyTransferable(asset: DesignSystemItem): Transferable {
    return object : Transferable {
        override fun getTransferData(flavor: DataFlavor?): Any {
            return when (flavor) {
                DESIGN_SYSTEM_URL_FLAVOR -> asset
                DataFlavor.stringFlavor -> asset.toString()
                else -> UnsupportedFlavorException(flavor)
            }
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = transferDataFlavors.contains(flavor)

        override fun getTransferDataFlavors(): Array<DataFlavor> = SUPPORTED_DATA_FLAVORS
    }
}
