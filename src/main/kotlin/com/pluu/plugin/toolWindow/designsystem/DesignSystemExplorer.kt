package com.pluu.plugin.toolWindow.designsystem

import com.android.tools.adtui.stdui.registerActionShortCutSet
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.util.Disposer
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerToolbar
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerToolbarViewModel
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerView
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerViewModel
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemType
import org.jetbrains.android.facet.AndroidFacet
import java.awt.BorderLayout
import javax.swing.JPanel
import kotlin.properties.Delegates

internal const val DESIGN_RES_MANAGER_PREF_KEY = "DesignResourceManagerPrefKey"

class DesignSystemExplorer private constructor(
    facet: AndroidFacet,
    private val designSystemExplorerViewModel: DesignSystemExplorerViewModel,
    designSystemExplorerView: DesignSystemExplorerView,
    private val toolbarViewModel: DesignSystemExplorerToolbarViewModel,
    toolbar: DesignSystemExplorerToolbar,
) : JPanel(BorderLayout()), Disposable {

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

        registerActionShortCutSet(
            action = { toolbarViewModel.requestSearch() },
            CommonShortcuts.getFind()
        )
    }

    private fun updateFacet(facet: AndroidFacet) {
        designSystemExplorerViewModel.facet = facet
//        resourceImportDragTarget.facet = facet
        toolbarViewModel.facet = facet
    }

    private fun updateResourceType(resourceType: DesignSystemType?) {
        toolbarViewModel.resourceType = resourceType
    }

    override fun dispose() {}

    companion object {
        /**
         * Create a new instance of [DesignSystemExplorer] optimized to be used in a [com.intellij.openapi.wm.ToolWindow]
         */
        @JvmStatic
        fun createForToolWindow(facet: AndroidFacet): DesignSystemExplorer {
            val explorerViewModel = DesignSystemExplorerViewModel.createViewModel(facet)
            val toolbarViewModel = DesignSystemExplorerToolbarViewModel(
                facet,
                explorerViewModel.tabs.getOrNull(explorerViewModel.supportTypeIndex)?.filterType,
                explorerViewModel.filterOptions
            )
            val toolbar = DesignSystemExplorerToolbar.create(toolbarViewModel)
            val designSystemExplorerView = DesignSystemExplorerView(explorerViewModel, facet.module.project)
            return DesignSystemExplorer(
                facet,
                explorerViewModel,
                designSystemExplorerView,
                toolbarViewModel,
                toolbar
            )
        }
    }
}
