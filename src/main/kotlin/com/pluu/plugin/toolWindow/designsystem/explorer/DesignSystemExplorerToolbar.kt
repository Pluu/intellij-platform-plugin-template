package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import javax.swing.GroupLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

private const val SEARCH_FIELD_LABEL = "Search resources by name"
private const val MODULE_PREFIX = "Module: "

private val MIN_FIELD_SIZE = JBUI.scale(40)
private val PREF_FIELD_SIZE = JBUI.scale(125)
private val MAX_FIELD_SIZE = JBUI.scale(150)
private val BUTTON_SIZE = JBUI.size(20)
private val GAP_SIZE = JBUI.scale(10)
private val ACTION_BTN_SIZE get() = JBUI.scale(32)

class DesignSystemExplorerToolbar(
    private val toolbarViewModel: DesignSystemExplorerToolbarViewModel,
) : JPanel(), DataProvider by toolbarViewModel {

    private val searchAction = createSearchField()
    private val refreshAction = action(RefreshAction(toolbarViewModel))

    init {
        layout = GroupLayout(this)
        val groupLayout = layout as GroupLayout
        val sequentialGroup = groupLayout.createSequentialGroup()
            .addFixedSizeComponent(refreshAction, true)
            .addComponent(searchAction, MIN_FIELD_SIZE, PREF_FIELD_SIZE, Int.MAX_VALUE)

        val verticalGroup = groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(refreshAction)
            .addComponent(searchAction)

        groupLayout.setHorizontalGroup(sequentialGroup)
        groupLayout.setVerticalGroup(verticalGroup)

        border = JBUI.Borders.merge(
            JBUI.Borders.empty(4, 2),
            JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
            true
        )
        toolbarViewModel.updateUICallback = this::update
        update() // Update current module right away.
    }

    private fun update() {
        refreshAction.update()
    }

    private fun createSearchField() = SearchTextField(true).apply {
        isFocusable = true
        toolTipText = SEARCH_FIELD_LABEL
        accessibleContext.accessibleName = SEARCH_FIELD_LABEL
        textEditor.columns = GAP_SIZE
        textEditor.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                toolbarViewModel.searchString = e.document.getText(0, e.document.length)
            }
        })
    }

    companion object {
        /**
         * Returns a [DesignSystemExplorerToolbar].
         */
        @JvmStatic
        fun create(toolbarViewModel: DesignSystemExplorerToolbarViewModel): DesignSystemExplorerToolbar {
            return DesignSystemExplorerToolbar(toolbarViewModel)
        }
    }
}

/**
 * Action to refresh the previews of a particular type of resources.
 */
private class RefreshAction(
    val viewModel: DesignSystemExplorerToolbarViewModel
) : AnAction(
    "Refresh Previews",
    "Refresh previews for ${viewModel.resourceType.displayName}s",
    AllIcons.Actions.Refresh
) {
    override fun actionPerformed(e: AnActionEvent) {
        viewModel.populateResourcesCallback()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.text = templatePresentation.text
        e.presentation.description = templatePresentation.description
        e.presentation.isEnabled = true
    }
}

private fun action(
    addAction: AnAction
) = ActionButton(addAction, PresentationFactory().getPresentation(addAction), "", BUTTON_SIZE)

private fun GroupLayout.SequentialGroup.addFixedSizeComponent(
    jComponent: JComponent,
    baseline: Boolean = false
): GroupLayout.SequentialGroup {
    val width = jComponent.preferredSize.width
    this.addComponent(baseline, jComponent, width, width, width)
    return this
}