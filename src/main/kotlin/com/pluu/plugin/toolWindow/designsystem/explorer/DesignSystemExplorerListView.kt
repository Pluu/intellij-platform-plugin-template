package com.pluu.plugin.toolWindow.designsystem.explorer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/explorer/ResourceExplorerListView.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.ui.resourcemanager.widget.LinkLabelSearchView
import com.intellij.concurrency.JobScheduler
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.util.ModalityUiUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.explorer.drag.resourceDragHandler
import com.pluu.plugin.toolWindow.designsystem.importer.DesignAssetImporter
import com.pluu.plugin.toolWindow.designsystem.importer.ResourceImportDialog
import com.pluu.plugin.toolWindow.designsystem.importer.ResourceImportDialogViewModel
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSection
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.RESOURCE_DESIGN_ASSETS_KEY
import com.pluu.plugin.toolWindow.designsystem.widget.Section
import com.pluu.plugin.toolWindow.designsystem.widget.SectionList
import com.pluu.plugin.toolWindow.designsystem.widget.SectionListModel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextAttribute
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.LayoutFocusTraversalPolicy
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
) : JPanel(BorderLayout()), Disposable, DataProvider {

    private var updatePending = false

    private var populateResourcesFuture: CompletableFuture<List<DesignSection>>? = null

    /** Reference to the last [CompletableFuture] used to search for filtered resources in other modules */
    private var searchFuture: CompletableFuture<List<DesignSection>>? = null
    private var showLoadingFuture: ScheduledFuture<*>? = null

    private var fileToSelect: VirtualFile? = null
    private var resourceToSelect: String? = null

    private val sectionListModel: SectionListModel = SectionListModel()
    private val dragHandler = resourceDragHandler()

    private val sectionList: SectionList = SectionList(sectionListModel).apply {
        border = SECTION_LIST_BORDER
        background = LIST_MODE_BACKGROUND
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
    }

    private val contentSeparator = JSeparator().apply {
        isVisible = false
        minimumSize = Dimension(JBUI.scale(10), JBUI.scale(4))
        maximumSize = Dimension(Integer.MAX_VALUE, JBUI.scale(10))
    }

    /** A view to hold clickable labels to change modules when filtering resources. */
    private val moduleSearchView = if (withMultiModuleSearch) LinkLabelSearchView().apply {
        backgroundColor = LIST_MODE_BACKGROUND
    } else null

    private val contentPanel = JPanel().apply {
        layout = BoxLayout(this@apply, BoxLayout.Y_AXIS)
        background = LIST_MODE_BACKGROUND
        add(sectionList)
        if (moduleSearchView != null) {
            add(contentSeparator)
            add(moduleSearchView)
        }
    }

    /**
     * Mouse listener to invoke the popup menu.
     *
     * This custom implementation is needed to ensure that the clicked element is selected
     * before invoking the menu.
     */
    private val popupHandler = object : PopupHandler() {
        val actionManager = ActionManager.getInstance()

        val actionGroup = DefaultActionGroup().apply {
            addAll(
                ActionCopyFrom(IdeActions.ACTION_COPY) { item ->
                    CopyPasteManager.getInstance().setContents(StringSelection(item.sampleCode))
                },
                EditComponentAction { item ->
                    // TODO: 데이터 중복 대응 필요
                    ResourceImportDialog(
                        project,
                        ResourceImportDialogViewModel(project, sequenceOf(item)) {
                            populateResourcesLists(keepScrollPosition = true)
                        }.apply {
                            modifyAssetItem = item
                        }
                    ).show()
                },
                ActionCopyFrom(IdeActions.ACTION_DELETE) { item ->
                    val result = Messages.showYesNoDialog(
                        project,
                        "Delete \"${item.name}\"'s data?",
                        item.name,
                        Messages.getQuestionIcon()
                    )
                    if (result == Messages.OK) {
                        DesignAssetImporter().removeDesignAsset(item, project, true)
                        populateResourcesLists(keepScrollPosition = true)
                    }
                },
            )
        }

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

    /**
     * A mouse listener that opens a detail file when double clicking
     * on an item from the list.
     * @see doSelectAssetAction
     */
    private val mouseClickListener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (!(e.clickCount == 2 && e.button == MouseEvent.BUTTON1)) {
                return
            }
            val assetListView = e.source as AssetListView
            val index = assetListView.locationToIndex(e.point)
            if (index >= 0) {
                val designAssetSet = assetListView.model.getElementAt(index)
                doSelectAssetAction(designAssetSet)
            }
        }
    }

    /**
     * Replace the content of the view with a [DesignAssetSet] for the provided [designAssetSet].
     */
    private fun doSelectAssetAction(designAssetSet: DesignAssetSet) {
        val asset = designAssetSet.asset
        viewModel.doSelectAssetAction(asset)
    }

    init {
        viewModel.updateUiCallback = { reason ->
            when (reason) {
                UpdateUiReason.IMAGE_CACHE_CHANGED -> repaint()
                UpdateUiReason.DESIGN_SYSTEM_TYPE_CHANGED -> {
                    setContentPanel()
                    populateResourcesLists()
                }

                UpdateUiReason.DESIGN_SYSTEM_CHANGED -> {
                    populateResourcesLists(keepScrollPosition = true)
                }
            }
        }
        populateResourcesLists()
        viewModel.speedSearch.addChangeListener {
            sectionList.getLists().filterIsInstance<AssetListView>()
                .forEach { assetListView -> assetListView.refilter() }
            sectionList.getSections().filterIsInstance<AssetSection<AssetListView>>().forEach { section ->
                section.updateHeaderName((section.list as? AssetListView)?.getFilteredSize())
            }
            contentPanel.validate()
            contentPanel.repaint()
        }

        setContentPanel()
        isFocusTraversalPolicyProvider = true
        focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
            override fun getFirstComponent(p0: Container?): Component {
                return sectionList.getLists().firstOrNull() ?: this@DesignSystemExplorerListView
            }
        }
    }

    private fun setContentPanel() {
        // Because we are reusing the UI items, we should clear the selection
        // to avoid interaction issues
        sectionList.getLists().forEach { it.selectionModel.clearSelection() }
        removeAll()
        add(contentPanel)
        revalidate()
        repaint()
    }

    private fun getSelectedAssets(): List<DesignSystemItem> {
        return sectionList.getLists()
            .flatMap { it.selectedValuesList }
            .filterIsInstance<DesignAssetSet>()
            .map { it.asset }
    }

    /**
     * Update the [sectionList] to show the current lists of resource. By default, the scroll
     * position will be reset to the top.
     *
     * @param keepScrollPosition: when true, the updated list will be automatically scrolled to
     *  the position it had before. This is the desired behaviour in some particular scenarios,
     *  and it is the caller's responsibility to decide depending on the context.
     */
    private fun populateResourcesLists(keepScrollPosition: Boolean = false) {
        val selectedValue = sectionList.selectedValue
        val selectedIndices = sectionList.selectedIndices
        val scrollPosition = getScrollPosition()
        updatePending = true
        populateResourcesFuture?.cancel(true)
        populateResourcesFuture = viewModel.getDesignSections()
            .whenCompleteAsync({ resourceLists, _ ->
                updatePending = false
                displayResources(resourceLists)
                if (keepScrollPosition) setScrollPosition(scrollPosition)
                selectIndicesIfNeeded(selectedValue, selectedIndices)
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
        sectionListModel.clear()
        sectionListModel.addSection(createLoadingSection())
    }

    private fun displayResources(resourceLists: List<DesignSection>) {
        sectionListModel.clear()
        val sections = resourceLists
            .filterNot { it.assetSets.isEmpty() }
            .map(this::createSection)
            .toList()
        if (sections.isNotEmpty()) {
            sectionListModel.addSections(sections)
        } else {
            sectionListModel.addSection(createEmptySection())
        }
        sectionList.validate()
        sectionList.repaint()
    }

    private fun createLoadingSection() = AssetSection<DesignAssetSet>(
        viewModel.selectedTabName, null,
        AssetListView(emptyList(), null).apply {
            setPaintBusy(true)
            setEmptyText("Loading...")
            background = this@DesignSystemExplorerListView.background
        }
    )

    private fun createEmptySection() = AssetSection<DesignAssetSet>(
        viewModel.selectedTabName, null,
        AssetListView(emptyList(), null).apply {
            setEmptyText("No ${viewModel.selectedTabName} available")
            background = this@DesignSystemExplorerListView.background
        }
    )

    private fun selectIndicesIfNeeded(selectedValue: Any?, selectedIndices: List<IntArray?>) {
        val finalFileToSelect = fileToSelect
        val finalResourceToSelect = resourceToSelect
        if (finalFileToSelect != null) {
            // Attempt to select resource by file, if it was pending.
            selectAsset(finalFileToSelect)
        } else if (finalResourceToSelect != null) {
            // Attempt to select resource by name, if it was pending.
            selectAsset(finalResourceToSelect, recentlyAdded = false)
        } else if (selectedValue != null) {
            // Attempt to reselect the previously selected element
            // If the value still exist in the list, just reselect it
            sectionList.selectedValue = selectedValue

            // Otherwise, like if the selected resource was renamed, we reselect the element
            // based on the indexes
            if (sectionList.selectedIndex == null) {
                sectionList.selectedIndices = selectedIndices
            }
        }

        // Guarantee that any other pending selection is cancelled. Having more than one of these is unintended behavior.
        fileToSelect = null
        resourceToSelect = null
    }

    private fun getScrollPosition(): Point {
        return sectionList.viewport.viewPosition
    }

    private fun setScrollPosition(scrollPosition: Point) {
        sectionList.viewport.viewPosition = scrollPosition
    }

    /**
     * Selects a [DesignAssetSet] by a given [VirtualFile]. Depending of the file, the currently displayed resource type may change to
     * select the right resource.
     */
    fun selectAsset(virtualFile: VirtualFile) {
        if (virtualFile.isDirectory) return

        if (updatePending) {
            fileToSelect = virtualFile
        } else {
            doSelectAsset { assetSet ->
                (assetSet.asset.file == virtualFile).also { if (it) fileToSelect = null }
            }
        }
    }

    /**
     * Selects a listed [DesignAssetSet] by its name.
     *
     * @param resourceName Name to look for in existing lists of resources.
     * @param recentlyAdded The resource might not be listed yet if it was recently added (awaiting resource notification).
     */
    fun selectAsset(resourceName: String, recentlyAdded: Boolean) {
        if (updatePending || recentlyAdded) {
            resourceToSelect = resourceName
        }
        if (!updatePending) {
            doSelectAsset isAsset@{ assetSet ->
                val found = assetSet.name == resourceName
                if (found) {
                    resourceToSelect = null
                }
                return@isAsset found
            }
        }
    }

    private fun doSelectAsset(isDesiredAssetSet: (DesignAssetSet) -> Boolean) {
        sectionList.getLists()
            .filterIsInstance<AssetListView>()
            .forEachIndexed { listIndex, list ->
                for (assetIndex in 0 until list.model.size) {
                    if (isDesiredAssetSet(list.model.getElementAt(assetIndex))) {
                        sectionList.selectedIndex = listIndex to assetIndex
                        sectionList.scrollToSelection()
                        list.requestFocusInWindow()
                        return
                    }
                }
            }
    }

    private fun createSection(section: DesignSection): AssetSection<DesignAssetSet> {
        val assetList = AssetListView(
            section.assetSets,
            viewModel.speedSearch,
            viewModel.filterOptions.sampleImageSize
        ).apply {
            cellRenderer = DesignAssetCellRenderer(viewModel.assetPreviewManager, section.isVisibleTypeName)
            dragHandler.registerSource(this)
            addMouseListener(popupHandler)
            addMouseListener(mouseClickListener)
//            addKeyListener(keyListener)
            selectionMode = ListSelectionModel.SINGLE_SELECTION
//            this.addListSelectionListener {
//                listeners.forEach { listener ->
//                    listener.onDesignAssetSetSelected(sectionList.selectedValue as? ResourceAssetSet)
//                }
//                (sectionList.selectedValue as? ResourceAssetSet)?.let { viewModel.updateSelectedAssetSet(it) }
//                updateSummaryPreview()
//            }

        }
        return AssetSection(section.name, assetList.getFilteredSize(), assetList)
    }

    private class AssetSection<T>(
        override var name: String,
        size: Int?,
        override var list: JList<T>
    ) : Section<T> {

        private val headerNameLabel = JBLabel(buildName(size)).apply {
            font = SECTION_HEADER_LABEL_FONT
            border = JBUI.Borders.empty(8, 0)
        }

        override var header: JComponent = createHeaderComponent()

        fun updateHeaderName(newSize: Int?) {
            headerNameLabel.text = buildName(newSize)
        }

        private fun createHeaderComponent() = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(headerNameLabel, BorderLayout.WEST)
            border = SECTION_HEADER_BORDER
        }

        private fun buildName(size: Int?): String {
            val itemNumber = size?.let { " ($it)" } ?: ""
            return "Result$itemNumber"
        }
    }

    override fun getData(dataId: String): Any? {
        return viewModel.getData(dataId, getSelectedAssets())
    }

    override fun dispose() {
        populateResourcesFuture?.cancel(true)
        searchFuture?.cancel(true)
        showLoadingFuture?.cancel(true)
    }

    private class EditComponentAction(
        private val action: (DesignSystemItem) -> Unit
    ) : AnAction(
        "Edit...",
        "Edit component",
        AllIcons.Actions.Edit
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val assets = e.getData(RESOURCE_DESIGN_ASSETS_KEY)?.firstOrNull() ?: return
            action(assets)
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

    private class ActionCopyFrom(
        actionId: String,
        private val action: (DesignSystemItem) -> Unit
    ) : AnAction() {
        init {
            ActionUtil.copyFrom(this, actionId)
        }

        override fun actionPerformed(e: AnActionEvent) {
            val assets = e.getData(RESOURCE_DESIGN_ASSETS_KEY)?.firstOrNull() ?: return
            action(assets)
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }
}