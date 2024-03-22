package com.pluu.plugin.toolWindow.designsystem.model

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/model/ResourceDataManager.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

/**
 * [DataFlavor] for [DesignSystemItem]
 */
@JvmField
val DESIGN_SYSTEM_URL_FLAVOR = DataFlavor(DesignSystemItem::class.java, "DesignSystem File Url")

private val SUPPORTED_DATA_FLAVORS = arrayOf(DESIGN_SYSTEM_URL_FLAVOR, DataFlavor.stringFlavor)

class ResourceDataManager(val project: Project) : CopyProvider {

    private var selectedItems: List<DesignSystemItem>? = null

    fun getData(dataId: String?, selectedAssets: List<DesignSystemItem>): Any? {
        this.selectedItems = selectedAssets
        return when (dataId) {
            PlatformDataKeys.COPY_PROVIDER.name -> this
            RESOURCE_DESIGN_ASSETS_KEY.name -> selectedAssets
            else -> null
        }
    }

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
        return getItemPsiFile(project, resourceItem)
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
