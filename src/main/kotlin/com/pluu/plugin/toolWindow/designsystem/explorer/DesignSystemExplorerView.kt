package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.widget.OverflowingTabbedPaneWrapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.concurrent.CompletableFuture
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane

class DesignSystemExplorerView(
    private val viewModel: DesignSystemExplorerViewModel,
) : JPanel(BorderLayout()), Disposable {

    private val resourcesTabsPanel = OverflowingTabbedPaneWrapper().apply {
        viewModel.supportedTypes.forEach {
            tabbedPane.add(it.displayName, null)
        }
        tabbedPane.selectedIndex = 0
        tabbedPane.addChangeListener { event ->
            val index = (event.source as JTabbedPane).selectedIndex
            viewModel.supportTypeIndex = index
            this.requestFocus()
        }
    }

    private val topActionsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }

    private val headerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(resourcesTabsPanel)
        add(topActionsPanel)
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
            listView = createResourcesListView(listViewModel).also {
                if (!Disposer.isDisposed(this)) {
                    centerPanel.removeAll()
                    centerPanel.add(it)
                    Disposer.register(this, it)
                } else {
                    Disposer.dispose(it)
                }
            }
            // selectIfNeeded()
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

    private fun createResourcesListView(viewModel: DesignSystemExplorerListViewModel): DesignSystemExplorerListView {
        return DesignSystemExplorerListView(
            viewModel
        )
    }
}