package com.pluu.plugin.toolWindow.designsystem.explorer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/explorer/ResourceExplorerToolbar.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.explorer.action.HeaderAction
import com.pluu.plugin.toolWindow.designsystem.explorer.action.PopupAction
import com.pluu.plugin.toolWindow.designsystem.importer.ResourceImportDialog
import com.pluu.plugin.toolWindow.designsystem.importer.ResourceImportDialogViewModel
import com.pluu.plugin.toolWindow.designsystem.model.FilterImageSize
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

private const val SEARCH_FIELD_LABEL = "Search sample by name"
private const val FILTERS_BUTTON_LABEL = "Filter displayed sample"

private val BUTTON_SIZE = JBUI.size(30, 20)
private val GAP_SIZE = JBUI.scale(10)

class DesignSystemExplorerToolbar(
    private val toolbarViewModel: DesignSystemExplorerToolbarViewModel,
) : JPanel(), DataProvider by toolbarViewModel {

    private val searchAction = createSearchField()
    private val refreshAction = action(RefreshAction(toolbarViewModel))

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        val addAction = action(NewSampleAction(toolbarViewModel))
        val filterAction = action(FilterAction(toolbarViewModel))

        add(addAction)
        add(refreshAction)
        add(searchAction)
        add(filterAction)

        border = JBUI.Borders.merge(
            JBUI.Borders.empty(4, 2),
            JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
            true
        )
        toolbarViewModel.updateUICallback = this::update
        toolbarViewModel.requestSearch = this::requestSearch
        update() // Update current module right away.
    }

    private fun update() {
        refreshAction.update()
    }

    private fun requestSearch() {
        searchAction.requestFocusInWindow()
    }

    private fun createSearchField() = SearchTextField(true).apply {
        isFocusable = true
        toolTipText = SEARCH_FIELD_LABEL
        accessibleContext.accessibleName = SEARCH_FIELD_LABEL
        textEditor.columns = GAP_SIZE
        addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                toolbarViewModel.searchString = this@apply.text
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

private class ImageSizeFilterAction(
    val viewModel: DesignSystemExplorerToolbarViewModel,
    private val typeFilter: FilterImageSize
) : ToggleAction(
    typeFilter.name,
    "Filter ${typeFilter.name}",
    null
) {
    override fun isSelected(e: AnActionEvent): Boolean {
        return viewModel.sampleImageSize == typeFilter
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        viewModel.sampleImageSize = typeFilter
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

/**
 * Action to refresh the previews of a particular type of resources.
 */
private class RefreshAction(
    val viewModel: DesignSystemExplorerToolbarViewModel
) : AnAction(
    "Refresh Previews",
    "Refresh previews for ${viewModel.resourceType?.name ?: "ALL item"}s",
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

private class FilterAction(
    val viewModel: DesignSystemExplorerToolbarViewModel
) : PopupAction(AllIcons.General.Filter, FILTERS_BUTTON_LABEL) {
    override fun createAddPopupGroup() = DefaultActionGroup().apply {
        addRelatedTypeFilterActions(viewModel)
    }
}

private fun action(
    addAction: AnAction
) = ActionButton(addAction, PresentationFactory().getPresentation(addAction), "", BUTTON_SIZE)

private fun DefaultActionGroup.addRelatedTypeFilterActions(viewModel: DesignSystemExplorerToolbarViewModel) {
    // Group the supported filters by their display name. So that one menu-item applies to the related filters.
    val supportedImageSize = viewModel.typeFiltersModel.getSupportedImageSize().toList()
    if (supportedImageSize.isNotEmpty()) {
        addSeparator()
        val header = "By Image Size Type"
        add(HeaderAction(header, header))
        supportedImageSize.forEach { filters ->
            add(ImageSizeFilterAction(viewModel, filters))
        }
    }
}

private class NewSampleAction(
    val viewModel: DesignSystemExplorerToolbarViewModel
) : AnAction(
    "New Sample Design System",
    "New sample from disk",
    AllIcons.General.Add
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = viewModel.facet.module.project
        ResourceImportDialog(
            project,
            ResourceImportDialogViewModel(project, emptySequence()) {
                viewModel.populateResourcesCallback()
            }
        ).show()
    }
}
