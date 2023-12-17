package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import java.awt.event.ItemEvent
import javax.swing.GroupLayout
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.PopupMenuEvent

private const val SEARCH_FIELD_LABEL = "Search resources by name"
private const val ADD_BUTTON_LABEL = "Add resources to the module"
private const val FILTERS_BUTTON_LABEL = "Filter displayed resources"
private const val MODULE_PREFIX = "Module: "

private val MIN_FIELD_SIZE = JBUI.scale(40)
private val PREF_FIELD_SIZE = JBUI.scale(125)
private val MAX_FIELD_SIZE = JBUI.scale(150)
private val BUTTON_SIZE = JBUI.size(20)
private val GAP_SIZE = JBUI.scale(10)
private val ACTION_BTN_SIZE get() = JBUI.scale(32)

class DesignSystemExplorerToolbar(
    private val toolbarViewModel: DesignSystemExplorerToolbarViewModel,
    private val moduleSelectionCombo: ComboBox<String>
) : JPanel(), DataProvider by toolbarViewModel {

    private val searchAction = createSearchField()

    init {
        layout = GroupLayout(this)
        val groupLayout = layout as GroupLayout
        val sequentialGroup = groupLayout.createSequentialGroup()
            .addComponent(moduleSelectionCombo, MIN_FIELD_SIZE, PREF_FIELD_SIZE, MAX_FIELD_SIZE)
            .addComponent(searchAction, MIN_FIELD_SIZE, PREF_FIELD_SIZE, Int.MAX_VALUE)

        val verticalGroup = groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(moduleSelectionCombo)
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
        moduleSelectionCombo.selectedItem = toolbarViewModel.currentModuleName
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
        fun create(
            toolbarViewModel: DesignSystemExplorerToolbarViewModel,
            moduleComboEnabled: Boolean
        ): DesignSystemExplorerToolbar {
            val moduleSelectionCombo = createModuleSelectionComboBox(toolbarViewModel, moduleComboEnabled)
            return DesignSystemExplorerToolbar(toolbarViewModel, moduleSelectionCombo)
        }
    }
}

/**
 * Creates a combo box for the [DesignSystemExplorerToolbar], should contain available modules in the project. Selecting a module should
 * change the working facet in the [DesignSystemExplorerToolbarViewModel].
 *
 * @param moduleComboEnabled Sets the isEnabled UI property. I.e: Whether it's allowed for the user to select a different module.
 */
private fun createModuleSelectionComboBox(
    toolbarViewModel: DesignSystemExplorerToolbarViewModel,
    moduleComboEnabled: Boolean
) =
    ComboBox<String>().apply {
        model = CollectionComboBoxModel(toolbarViewModel.getAvailableModules().toMutableList())
        isEnabled = moduleComboEnabled
        renderer = object : ColoredListCellRenderer<String>() {
            override fun customizeCellRenderer(
                list: JList<out String>,
                value: String,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                append(MODULE_PREFIX + value)
            }
        }

        addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val moduleName = event.itemSelectable.selectedObjects.first() as String
                toolbarViewModel.onModuleSelected(moduleName)
            }
        }

        addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                (model as CollectionComboBoxModel).replaceAll(toolbarViewModel.getAvailableModules())
            }
        })
    }