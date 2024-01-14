package com.pluu.plugin.toolWindow.designsystem

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.util.Disposer
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerToolbar
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerToolbarViewModel
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerView
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerViewModel
import org.jetbrains.android.facet.AndroidFacet
import java.awt.BorderLayout
import javax.swing.JPanel
import kotlin.properties.Delegates

internal const val DESIGN_RES_MANAGER_PREF_KEY = "DesignResourceManagerPrefKey"

class DesignSystemExplorer private constructor(
    facet: AndroidFacet,
    private val designSystemExplorerViewModel: DesignSystemExplorerViewModel,
    private val designSystemExplorerView: DesignSystemExplorerView,
    private val toolbarViewModel: DesignSystemExplorerToolbarViewModel,
    private val toolbar: DesignSystemExplorerToolbar,
) : JPanel(BorderLayout()), Disposable, DataProvider {

    var facet by Delegates.observable(facet) { _, _, newValue -> updateFacet(newValue) }

    init {
        toolbarViewModel.facetUpdaterCallback = { newValue -> this.facet = newValue }
        toolbarViewModel.refreshResourcesPreviewsCallback = { designSystemExplorerViewModel.refreshPreviews() }
        toolbarViewModel.populateResourcesCallback = { designSystemExplorerViewModel.refreshListModel() }
        designSystemExplorerViewModel.facetUpdaterCallback = { newValue -> this.facet = newValue }
        designSystemExplorerViewModel.designSystemTypeUpdaterCallback = this::updateResourceType

        val centerContainer = JPanel(BorderLayout())
        centerContainer.add(toolbar, BorderLayout.NORTH)
        centerContainer.add(designSystemExplorerView)
        add(centerContainer, BorderLayout.CENTER)
        Disposer.register(this, designSystemExplorerViewModel)
        Disposer.register(this, designSystemExplorerView)
    }

    private fun updateFacet(facet: AndroidFacet) {
        designSystemExplorerViewModel.facet = facet
//        resourceImportDragTarget.facet = facet
        toolbarViewModel.facet = facet
    }

    private fun updateResourceType(resourceType: DesignSystemType) {
        toolbarViewModel.resourceType = resourceType
    }

    override fun dispose() {}

    override fun getData(dataId: String): Any? {
        return null
    }

    companion object {
        /**
         * Create a new instance of [DesignSystemExplorer] optimized to be used in a [com.intellij.openapi.wm.ToolWindow]
         */
        @JvmStatic
        fun createForToolWindow(facet: AndroidFacet): DesignSystemExplorer {
            val designSystemExplorerViewModel = DesignSystemExplorerViewModel.createViewModel(facet)
            val toolbarViewModel = DesignSystemExplorerToolbarViewModel(
                facet,
                designSystemExplorerViewModel.supportedTypes[designSystemExplorerViewModel.supportTypeIndex],
                designSystemExplorerViewModel.filterOptions
            )
            val toolbar = DesignSystemExplorerToolbar.create(toolbarViewModel)
            val designSystemExplorerView = DesignSystemExplorerView(designSystemExplorerViewModel, facet)
            return DesignSystemExplorer(
                facet,
                designSystemExplorerViewModel,
                designSystemExplorerView,
                toolbarViewModel,
                toolbar
            )
        }
    }
}
