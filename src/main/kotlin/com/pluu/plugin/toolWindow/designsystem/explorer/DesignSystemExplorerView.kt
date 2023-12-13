package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.widget.OverflowingTabbedPaneWrapper
import com.intellij.openapi.Disposable
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel

class DesignSystemExplorerView(
) : JPanel(BorderLayout()), Disposable {

    private val resourcesTabsPanel = OverflowingTabbedPaneWrapper().apply {
        DesignSystemType.values().forEach {
            tabbedPane.add(it.displayName, null)
        }
        tabbedPane.selectedIndex = 0
        tabbedPane.addChangeListener { event ->
//            val index = (event.source as JTabbedPane).model.selectedIndex
//            viewModel.resourceTypeIndex = index
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

    init {
        add(getContentPanel())
    }

    private fun getContentPanel(): JPanel {
        val explorerListPanel = JPanel(BorderLayout()).apply {
            add(headerPanel, BorderLayout.NORTH)
            add(centerPanel, BorderLayout.CENTER)
        }

        return explorerListPanel
    }

    override fun dispose() {
//        TODO("Not yet implemented")
    }
}