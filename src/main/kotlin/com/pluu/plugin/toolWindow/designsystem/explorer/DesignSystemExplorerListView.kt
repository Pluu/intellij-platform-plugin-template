package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.concurrency.JobScheduler
import com.intellij.ide.CopyProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import com.intellij.util.ModalityUiUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.explorer.drag.ResourceFilesTransferHandler
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSection
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.RESOURCE_DESIGN_ASSETS_KEY
import com.pluu.plugin.toolWindow.designsystem.widget.AssetView
import com.pluu.plugin.toolWindow.designsystem.widget.RowAssetView
import java.awt.Component
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.DefaultListModel
import javax.swing.DropMode
import javax.swing.JList
import javax.swing.ListSelectionModel

/**
 * Delay to wait for before showing the "Loading" state.
 *
 * If we don't delay showing the loading state, user might see a quick flickering
 * when switching tabs because of the quick change from old resources to loading view to new resources.
 */
private const val MS_DELAY_BEFORE_LOADING_STATE = 100L // ms
private val UNIT_DELAY_BEFORE_LOADING_STATE = TimeUnit.MILLISECONDS

class DesignSystemExplorerListView(
    private val viewModel: DesignSystemExplorerListViewModel,
    val project: Project,
) : JBList<DesignAssetSet>(), Disposable, DataProvider, CopyProvider {

    var assetView: AssetView
        private set

    private var updatePending = false

    private var populateResourcesFuture: CompletableFuture<List<DesignAssetSet>>? = null

    /** Reference to the last [CompletableFuture] used to search for filtered resources in other modules */
    private var searchFuture: CompletableFuture<List<DesignSection>>? = null
    private var showLoadingFuture: ScheduledFuture<*>? = null

    private val listModel = DefaultListModel<DesignAssetSet>()

    private val popupHandler = object : PopupHandler() {
        val actionManager = ActionManager.getInstance()

        val actionGroup = DefaultActionGroup(CopyComponentAction())

        override fun invokePopup(comp: Component, x: Int, y: Int) {
            val list = comp as JList<*>
            // Select the element before invoking the popup menu
            val clickedIndex = list.locationToIndex(Point(x, y))
            if (!list.isSelectedIndex(clickedIndex)) {
                list.selectedIndex = clickedIndex
            }

            val popupMenu = actionManager.createActionPopupMenu("ResourceExplorer", actionGroup)
            popupMenu.setTargetComponent(list)
            val menu = popupMenu.component
            menu.show(comp, x, y)
        }
    }

    init {
        layoutOrientation = JList.VERTICAL
        cellRenderer = DesignAssetCellRenderer(viewModel.assetPreviewManager, true)

        dragEnabled = true
        dropMode = DropMode.ON
        transferHandler = ResourceFilesTransferHandler(this)

        // Popup Handler
        addMouseListener(popupHandler)

        setExpandableItemsEnabled(false)
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = 0

        model = listModel
        assetView = RowAssetView(viewModel.filterOptions.sampleImageSize)

        viewModel.updateUiCallback = { reason ->
            when (reason) {
                UpdateUiReason.IMAGE_CACHE_CHANGED -> {
                    repaint()
                }

                UpdateUiReason.DESIGN_SYSTEM_TYPE_CHANGED -> {
                    populateResourcesLists()
                }

                UpdateUiReason.DESIGN_SYSTEM_CHANGED -> {
                    populateResourcesLists()
                }
            }
        }
        populateResourcesLists()
        isFocusTraversalPolicyProvider = true
    }

    private fun getSelectedAssets(): List<DesignSystemItem> {
        return listModel.elements().toList()
            .map {
                it.asset
            }
    }

    private fun populateResourcesLists() {
        updatePending = true
        populateResourcesFuture?.cancel(true)
        populateResourcesFuture = viewModel.getDesignAssetSets()
            .whenCompleteAsync({ resourceLists, _ ->
                updatePending = false
                displayResources(resourceLists)
            }, EdtExecutorService.getInstance())

        if (populateResourcesFuture?.isDone == false) {
            if (showLoadingFuture == null) {
                showLoadingFuture = JobScheduler.getScheduler().schedule(
                    { ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState(), this::displayLoading) },
                    MS_DELAY_BEFORE_LOADING_STATE,
                    UNIT_DELAY_BEFORE_LOADING_STATE
                )
            }
        }
    }

    private fun displayLoading() {
        showLoadingFuture = null
        if (populateResourcesFuture?.isDone != false) {
            return
        }
        listModel.clear()
        setEmptyText("Loading...")
    }

    private fun displayResources(resourceLists: List<DesignAssetSet>) {
        listModel.clear()
        listModel.addAll(resourceLists)
        revalidate()
        repaint()
    }

    override fun dispose() {
        populateResourcesFuture?.cancel(true)
        searchFuture?.cancel(true)
        showLoadingFuture?.cancel(true)
    }

    override fun getData(dataId: String): Any? {
        return when (dataId) {
            RESOURCE_DESIGN_ASSETS_KEY.name -> {
                selectedValuesList.map { it.asset }
            }

            PlatformDataKeys.COPY_PROVIDER.name -> this
            else -> null
        }
    }

    override fun performCopy(dataContext: DataContext) {
        val item = dataContext.getData(RESOURCE_DESIGN_ASSETS_KEY)?.firstOrNull() ?: return
        CopyPasteManager.getInstance().setContents(StringSelection(item.sampleCode))
    }

    override fun isCopyVisible(dataContext: DataContext): Boolean = isCopyEnabled(dataContext)

    override fun isCopyEnabled(dataContext: DataContext): Boolean = selectedValuesList.isNotEmpty()

    class CopyComponentAction : AnAction() {
        init {
            ActionUtil.copyFrom(this, IdeActions.ACTION_COPY)
        }

        override fun actionPerformed(dataContext: AnActionEvent) {
            val item = dataContext.getData(RESOURCE_DESIGN_ASSETS_KEY)?.firstOrNull() ?: return
            CopyPasteManager.getInstance().setContents(StringSelection(item.sampleCode))
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }
}