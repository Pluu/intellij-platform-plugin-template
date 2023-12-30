package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.PsiElement
import org.jetbrains.android.facet.AndroidFacet
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

/**
 * [DataFlavor] for [DesignSystemItem]
 */
@JvmField
val DESIGN_SYSTEM_URL_FLAVOR = DataFlavor(DesignSystemItem::class.java, "DesignSystem File Url")

private val SUPPORTED_DATA_FLAVORS = arrayOf(DESIGN_SYSTEM_URL_FLAVOR, DataFlavor.stringFlavor)

class ResourceDataManager(var facet: AndroidFacet) : CopyProvider {

    private var selectedItems: List<DesignSystemItem>? = null

    override fun performCopy(dataContext: DataContext) {
        selectedItems?.let {
            if (it.isNotEmpty()) {
                val designAsset = it.first()
                CopyPasteManager.getInstance().setContents(createTransferable(designAsset))
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun isCopyVisible(dataContext: DataContext): Boolean = isCopyEnabled(dataContext)

    override fun isCopyEnabled(dataContext: DataContext): Boolean = !selectedItems.isNullOrEmpty()

    /**
     * Try to find the psi element that this [DesignSystemItem] represents.
     */
    fun findPsiElement(resourceItem: DesignSystemItem): PsiElement? {
        return getItemPsiFile(facet.module.project, resourceItem)
    }
}

fun createTransferable(asset: DesignSystemItem): Transferable {
    return object : Transferable {
        override fun getTransferData(flavor: DataFlavor?): Any {
            return when (flavor) {
                DESIGN_SYSTEM_URL_FLAVOR -> asset
                DataFlavor.stringFlavor -> asset.toString()
                else -> UnsupportedFlavorException(flavor)
            }
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = flavor in SUPPORTED_DATA_FLAVORS

        override fun getTransferDataFlavors(): Array<DataFlavor> = SUPPORTED_DATA_FLAVORS
    }
}
