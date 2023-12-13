package com.pluu.plugin.toolWindow.designsystem

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerView
import org.jetbrains.android.facet.AndroidFacet
import java.awt.BorderLayout
import javax.swing.JPanel
import kotlin.properties.Delegates

class DesignSystemExplorer private constructor(
    facet: AndroidFacet,
    private val designSystemExplorerView: DesignSystemExplorerView,
) : JPanel(BorderLayout()), Disposable, DataProvider {

    var facet by Delegates.observable(facet) { _, _, newValue -> updateFacet(newValue) }

    init {
        val centerContainer = JPanel(BorderLayout())
        centerContainer.add(designSystemExplorerView)
        add(centerContainer, BorderLayout.CENTER)
        Disposer.register(this, designSystemExplorerView)
    }

    private fun updateFacet(facet: AndroidFacet) {
//        resourceExplorerViewModel.facet = facet
//        resourceImportDragTarget.facet = facet
//        toolbarViewModel.facet = facet
    }

    override fun dispose() {
        //  TODO("Not yet implemented")
    }

    override fun getData(dataId: String): Any? {
        // TODO("Not yet implemented")
        return null
    }

    companion object {
        private val DIALOG_PREFERRED_SIZE get() = JBUI.size(850, 620)

        /**
         * Create a new instance of [ResourceExplorer] optimized to be used in a [com.intellij.openapi.wm.ToolWindow]
         */
        @JvmStatic
        fun createForToolWindow(facet: AndroidFacet): DesignSystemExplorer {
            val designSystemExplorerView = DesignSystemExplorerView()

            return DesignSystemExplorer(
                facet,
                designSystemExplorerView
            )
        }
    }
}
