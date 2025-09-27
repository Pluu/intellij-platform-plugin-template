package com.pluu.plugin.toolWindow.designsystem.model

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/model/ResourceDataManager.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.PsiElement
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageView
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

class ResourceDataManager(var facet: AndroidFacet) {

    fun uiDataSnapshot(sink: DataSink, selectedAssets: List<DesignSystemItem>) {
        sink[PlatformDataKeys.COPY_PROVIDER] = object : CopyProvider {
            override fun performCopy(dataContext: DataContext) {
                val designAsset = selectedAssets.firstOrNull() ?: return
                CopyPasteManager.getInstance().setContents(createTransferable(designAsset))
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

            override fun isCopyVisible(dataContext: DataContext): Boolean = isCopyEnabled(dataContext)

            override fun isCopyEnabled(dataContext: DataContext): Boolean = selectedAssets.isNotEmpty()
        }
        sink[RESOURCE_DESIGN_ASSETS_KEY] = selectedAssets.toTypedArray()

        sink.lazy(LangDataKeys.PSI_ELEMENT) {
            if (selectedAssets.size != 1) null
            else assetsToArrayPsiElements(selectedAssets).firstOrNull()
        }
        sink.lazy(LangDataKeys.PSI_ELEMENT_ARRAY) {
            assetsToArrayPsiElements(selectedAssets)
        }
        sink.lazy(UsageView.USAGE_TARGETS_KEY) {
            getUsageTargets(assetsToArrayPsiElements(selectedAssets))
        }
    }

    private fun assetsToArrayPsiElements(assets: List<DesignSystemItem>): Array<out PsiElement> =
        assets.asSequence()
            .mapNotNull(this::findPsiElement)
            .filter { it.manager.isInProject(it) }
            .toList()
            .toTypedArray()

    /**
     * Try to find the psi element that this [DesignSystemItem] represents.
     */
    fun findPsiElement(resourceItem: DesignSystemItem): PsiElement? {
        return getItemPsiFile(facet.module.project, resourceItem)
    }

    private fun getUsageTargets(chosenElements: Array<out PsiElement>?): Array<UsageTarget?> {
        if (chosenElements != null) {
            val usageTargets = arrayOfNulls<UsageTarget>(chosenElements.size)
            for (i in chosenElements.indices) {
                usageTargets[i] = PsiElement2UsageTargetAdapter(chosenElements[i], true)
            }
            return usageTargets
        }
        return emptyArray()
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
