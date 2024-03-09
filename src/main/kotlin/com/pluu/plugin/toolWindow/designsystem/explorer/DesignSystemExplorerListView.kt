package com.pluu.plugin.toolWindow.designsystem.explorer

import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.util.ModalityUiUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSection
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.widget.AssetView
import com.pluu.plugin.toolWindow.designsystem.widget.RowAssetView
import java.awt.font.TextAttribute
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.ListSelectionModel

private val SECTION_HEADER_SECONDARY_COLOR get() = JBColor.border()

private val SECTION_HEADER_BORDER
    get() = BorderFactory.createCompoundBorder(
        JBUI.Borders.empty(2),
        JBUI.Borders.customLine(SECTION_HEADER_SECONDARY_COLOR, 0, 0, 1, 0)
    )

private val SECTION_LIST_BORDER get() = JBUI.Borders.empty()

private val SECTION_HEADER_LABEL_FONT
    get() = JBUI.Fonts.label().deriveFont(
        mapOf(
            TextAttribute.WEIGHT to TextAttribute.WEIGHT_SEMIBOLD,
            TextAttribute.SIZE to JBUI.scaleFontSize(14f)
        )
    )

private val LIST_MODE_BACKGROUND = UIUtil.getListBackground()

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
    withMultiModuleSearch: Boolean = true,
) : JBList<DesignAssetSet>(), Disposable, DataProvider {

    var assetView: AssetView
        private set

    private var updatePending = false

    private var populateResourcesFuture: CompletableFuture<List<DesignAssetSet>>? = null

    /** Reference to the last [CompletableFuture] used to search for filtered resources in other modules */
    private var searchFuture: CompletableFuture<List<DesignSection>>? = null
    private var showLoadingFuture: ScheduledFuture<*>? = null

    private val listModel = DefaultListModel<DesignAssetSet>()

    init {
        layoutOrientation = JList.VERTICAL
        cellRenderer = DesignAssetCellRenderer(viewModel.assetPreviewManager, true)
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

    override fun getData(dataId: String): Any? {
        return viewModel.getData(dataId, getSelectedAssets())
    }

    override fun dispose() {
        populateResourcesFuture?.cancel(true)
        searchFuture?.cancel(true)
        showLoadingFuture?.cancel(true)
    }
}