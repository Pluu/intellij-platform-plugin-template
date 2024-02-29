package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.widget.OverflowingTabbedPaneWrapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.concurrent.CompletableFuture
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane

internal class DesignSystemExplorerView(
    private val viewModel: DesignSystemExplorerViewModel,
    private val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val resourcesTabsPanel = OverflowingTabbedPaneWrapper().apply {
        viewModel.tabs.forEach {
            tabbedPane.add(it.name, null)
        }
        if (tabbedPane.tabCount > 0) {
            tabbedPane.selectedIndex = 0
        }
        tabbedPane.addChangeListener { event ->
            val index = (event.source as JTabbedPane).selectedIndex
            viewModel.supportTypeIndex = index
            this.requestFocus()
        }
    }

    private val centerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty()
    }

    private val root: JPanel = panel {
        row { cell(resourcesTabsPanel).align(AlignX.FILL) }
        row { cell(centerPanel).align(Align.FILL) }.resizableRow()
    }

    private var listView: DesignSystemExplorerListView? = null

    private var listViewJob: CompletableFuture<DesignSystemExplorerListViewModel>? = null

    init {
        add(root)

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
            listView = createResourcesListView(listViewModel, project).also {
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

    private fun createResourcesListView(
        viewModel: DesignSystemExplorerListViewModel,
        project: Project
    ): DesignSystemExplorerListView {
        return DesignSystemExplorerListView(viewModel, project)
    }
}