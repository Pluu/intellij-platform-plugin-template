package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.widget.OverflowingTabbedPaneWrapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.JBUI
import org.jetbrains.android.facet.AndroidFacet
import java.awt.BorderLayout
import java.util.concurrent.CompletableFuture
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane

internal class DesignSystemExplorerView(
    private val viewModel: DesignSystemExplorerViewModel,
    private val facet: AndroidFacet
) : JPanel(BorderLayout()), Disposable {

    private val resourcesTabsPanel = OverflowingTabbedPaneWrapper().apply {
        viewModel.tabs.forEach {
            tabbedPane.add(it.name, null)
        }
        tabbedPane.selectedIndex = 0
        tabbedPane.addChangeListener { event ->
            val index = (event.source as JTabbedPane).selectedIndex
            viewModel.supportTypeIndex = index
            this.requestFocus()
        }
    }

    private val headerPanel: JPanel = panel {
        row { cell(resourcesTabsPanel).align(AlignX.FILL) }
    }

    private val centerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty()
    }

    private var listView: DesignSystemExplorerListView? = null

    private var listViewJob: CompletableFuture<DesignSystemExplorerListViewModel>? = null

    init {
        add(getContentPanel())

        viewModel.updateSupportTypeTabCallback = {
            resourcesTabsPanel.tabbedPane.selectedIndex = viewModel.supportTypeIndex
        }
        viewModel.populateResourcesCallback = {
            populateResources()
        }
        populateResources()
    }

    private fun populateResources() {
        listView?.let { Disposer.dispose(it) }
        listView = null
        listViewJob?.cancel(true)
        listViewJob = viewModel.createResourceListViewModel().whenCompleteAsync({ listViewModel, _ ->
            // TODO: Add a loading screen if this process takes too long.
            listView = createResourcesListView(listViewModel, facet).also {
                if (!Disposer.isDisposed(this)) {
                    centerPanel.removeAll()
                    centerPanel.add(it)
                    Disposer.register(this, it)
                } else {
                    Disposer.dispose(it)
                }
            }
        }, EdtExecutorService.getInstance())
    }

    override fun dispose() {
//        TODO("Not yet implemented")
    }

    private fun getContentPanel(): JPanel {
        val explorerListPanel = JPanel(BorderLayout()).apply {
            add(headerPanel, BorderLayout.NORTH)
            add(centerPanel, BorderLayout.CENTER)
        }

        return explorerListPanel
    }

    private fun createResourcesListView(
        viewModel: DesignSystemExplorerListViewModel,
        facet: AndroidFacet
    ): DesignSystemExplorerListView {
        return DesignSystemExplorerListView(viewModel, facet)
    }
}