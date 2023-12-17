package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.ui.resourcemanager.actions.ExpandAction
import com.android.tools.idea.ui.resourcemanager.model.ResourceAssetSet
import com.android.tools.idea.ui.resourcemanager.model.ResourceSection
import com.android.tools.idea.ui.resourcemanager.model.designAssets
import com.android.tools.idea.ui.resourcemanager.widget.LinkLabelSearchView
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollectionListModel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.speedSearch.NameFilteringListModel
import com.intellij.util.ModalityUiUtil
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.explorer.DesignSystemExplorerListViewModel.UpdateUiReason
import com.pluu.plugin.toolWindow.designsystem.findCompatibleFacets
import com.pluu.plugin.toolWindow.designsystem.widget.Section
import com.pluu.plugin.toolWindow.designsystem.widget.SectionList
import com.pluu.plugin.toolWindow.designsystem.widget.SectionListModel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Point
import java.awt.font.TextAttribute
import java.util.*
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

private val DEFAULT_LIST_MODE_WIDTH get() = JBUI.scale(60)
private val MAX_CELL_WIDTH get() = JBUI.scale(300)
private val LIST_CELL_SIZE get() = JBUI.scale(60)
private val MIN_CELL_WIDTH get() = JBUI.scale(150)
private val DEFAULT_CELL_WIDTH get() = LIST_CELL_SIZE
private val SECTION_HEADER_SECONDARY_COLOR get() = JBColor.border()

private val SECTION_HEADER_BORDER
    get() = BorderFactory.createCompoundBorder(
        JBUI.Borders.empty(4, 4, 8, 4),
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

private val TOOLBAR_BORDER
    get() = BorderFactory.createCompoundBorder(
        JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
        JBUI.Borders.empty(4, 2)
    )

private val GRID_MODE_BACKGROUND = UIUtil.getPanelBackground()
private val LIST_MODE_BACKGROUND = UIUtil.getListBackground()

/**
 * Delay to wait for before showing the "Loading" state.
 *
 * If we don't delay showing the loading state, user might see a quick flickering
 * when switching tabs because of the quick change from old resources to loading view to new resources.
 */
private const val MS_DELAY_BEFORE_LOADING_STATE = 100L // ms
private val UNIT_DELAY_BEFORE_LOADING_STATE = TimeUnit.MILLISECONDS

private const val PREVIEW_SIZE = "resourceExplorer.previewSize"

class DesignSystemExplorerListView(
    private val viewModel: DesignSystemExplorerListViewModel,
    withMultiModuleSearch: Boolean = true,
) : JPanel(BorderLayout()), Disposable, DataProvider {

    private var updatePending = false

    private var populateResourcesFuture: CompletableFuture<List<ResourceSection>>? = null

    /** Reference to the last [CompletableFuture] used to search for filtered resources in other modules */
    private var searchFuture: CompletableFuture<List<ResourceSection>>? = null
    private var showLoadingFuture: ScheduledFuture<*>? = null

    private var fileToSelect: VirtualFile? = null
    private var resourceToSelect: String? = null

    private var previewSize = DEFAULT_CELL_WIDTH
        set(value) {
            if (value != field) {
                PropertiesComponent.getInstance().setValue(PREVIEW_SIZE, value, DEFAULT_CELL_WIDTH)
                field = value
                sectionList.getLists().forEach {
                    (it as AssetListView).thumbnailWidth = previewSize
                }
            }
        }

    private val sectionListModel: SectionListModel = SectionListModel()

    private val topActionsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }

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

    private val centerPanel = JPanel().apply {
        layout = BoxLayout(this@apply, BoxLayout.Y_AXIS)
        background = LIST_MODE_BACKGROUND
        add(sectionList)
        if (moduleSearchView != null) {
            add(contentSeparator)
            add(moduleSearchView)
        }
    }

    private val contentPanel: JPanel =
        JPanel(BorderLayout()).apply {
            add(topActionsPanel, BorderLayout.NORTH)
            add(centerPanel)
        }

    init {
        viewModel.updateUiCallback = { reason ->
            when (reason) {
                UpdateUiReason.IMAGE_CACHE_CHANGED -> repaint()
                UpdateUiReason.DESIGN_SYSTEM_TYPE_CHANGED -> {
                    setContentPanel()
                    populateResourcesLists()
                    populateSearchLinkLabels()
                }

                UpdateUiReason.DESIGN_SYSTEM_CHANGED -> {
                    populateResourcesLists(keepScrollPosition = true)
                    populateSearchLinkLabels()
                }
            }
        }
        populateResourcesLists()
        populateSearchLinkLabels()
        viewModel.speedSearch.addChangeListener {
            sectionList.getLists().filterIsInstance<AssetListView>()
                .forEach { assetListView -> assetListView.refilter() }
            sectionList.getSections().filterIsInstance<AssetSection<AssetListView>>().forEach { section ->
                section.updateHeaderName((section.list as? AssetListView)?.getFilteredSize())
            }
            centerPanel.validate()
            centerPanel.repaint()
            populateSearchLinkLabels()
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

    private fun populateSearchLinkLabels() {
        if (moduleSearchView == null) return
        searchFuture?.let { future ->
            if (!future.isDone()) {
                // Only one 'future' for getOtherModulesResourceLists may run at a time.
                future.cancel(true)
            }
        }

        moduleSearchView.clear()
        contentSeparator.isVisible = false

        val filter = viewModel.speedSearch.filter
        if (filter.isNotBlank()) {
            searchFuture = viewModel.getOtherModulesResourceLists()
                .whenCompleteAsync({ resourceLists, _ ->
                    displaySearchLinkLabels(resourceLists, filter)
                }, EdtExecutorService.getInstance())
        }
        centerPanel.revalidate()
    }


    /**
     * Applies the filter in the SpeedSearch to the given resource sections, then, creates and displays LinkLabels to the modules with
     * resources matching the filter.
     *
     * @param filter Received filter string, since the filter in SpeedSearch might change at runtime while this is running.
     */
    private fun displaySearchLinkLabels(resourceSections: List<ResourceSection>, filter: String) {
        if (moduleSearchView == null) return // TODO: Log?
        val search = viewModel.speedSearch
        search.setEnabled(true)
        resourceSections.forEach { section ->
            val filteringModel = NameFilteringListModel(
                CollectionListModel(section.assetSets), { it.name }, search::shouldBeShowing,
                { StringUtil.notNullize(filter) })
            filteringModel.refilter()
            val resourcesCount = filteringModel.size
            if (resourcesCount > 0) {
                // TODO: Get the facet when the module is being set in ResourceExplorerViewModel by passing the module name instead of the actual facet.
                // I.e: This class should not be fetching module objects.
                findCompatibleFacets(viewModel.facet.module.project).firstOrNull {
                    it.module.name == section.libraryName
                }?.let { facetToChange ->
                    // Create [LinkLabel]s that when clicking them, changes the working module to the module in the given [AndroidFacet].
                    moduleSearchView.addLabel(
                        "$resourcesCount ${
                            StringUtil.pluralize(
                                "resource",
                                resourcesCount
                            )
                        } found in '${facetToChange.module.name}'"
                    ) {
                        viewModel.facetUpdated(facetToChange)
                    }
                }
            }
        }
        contentSeparator.isVisible = moduleSearchView.isVisible
        centerPanel.validate()
        centerPanel.repaint()
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
        populateResourcesFuture = viewModel.getCurrentModuleResourceLists()
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
        if (populateResourcesFuture?.isDone ?: true) {
            return
        }
        sectionListModel.clear()
        sectionListModel.addSection(createLoadingSection())
    }

    private fun displayResources(resourceLists: List<ResourceSection>) {
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

    private fun createLoadingSection() = AssetSection<ResourceAssetSet>(
        viewModel.facet.module.name, null,
        AssetListView(emptyList(), null).apply {
            setPaintBusy(true)
            setEmptyText("Loading...")
            background = this@DesignSystemExplorerListView.background
        }
    )

    private fun createEmptySection() = AssetSection<ResourceAssetSet>(
        viewModel.facet.module.name, null,
        AssetListView(emptyList(), null).apply {
            setEmptyText("No ${viewModel.selectedTabName.lowercase(Locale.US)} available")
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
     * Selects a [ResourceAssetSet] by a given [VirtualFile]. Depending of the file, the currently displayed resource type may change to
     * select the right resource.
     */
    fun selectAsset(virtualFile: VirtualFile) {
        if (virtualFile.isDirectory) return

        if (updatePending) {
            fileToSelect = virtualFile
        } else {
            doSelectAsset { assetSet ->
                assetSet.designAssets.any { it.file == virtualFile }.also { if (it) fileToSelect = null }
            }
        }
    }

    /**
     * Selects a listed [ResourceAssetSet] by its name.
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

    private fun doSelectAsset(isDesiredAssetSet: (ResourceAssetSet) -> Boolean) {
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

    private fun createSection(section: ResourceSection): AssetSection<ResourceAssetSet> {
        val assetList = AssetListView(section.assetSets, viewModel.speedSearch).apply {
            cellRenderer = DesignAssetCellRenderer(viewModel.assetPreviewManager)
//            dragHandler.registerSource(this)
//            addMouseListener(popupHandler)
//            addMouseListener(mouseClickListener)
//            addKeyListener(keyListener)
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            this.addListSelectionListener {
//                listeners.forEach { listener ->
//                    listener.onDesignAssetSetSelected(sectionList.selectedValue as? ResourceAssetSet)
//                }
//                (sectionList.selectedValue as? ResourceAssetSet)?.let { viewModel.updateSelectedAssetSet(it) }
//                updateSummaryPreview()
            }
            thumbnailWidth = this@DesignSystemExplorerListView.previewSize
            isGridMode = false
        }
        return AssetSection(section.libraryName, assetList.getFilteredSize(), assetList)
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

            val expandAction = object : ExpandAction() {
                override fun actionPerformed(e: AnActionEvent) {
                    super.actionPerformed(e)
                    list.isVisible = expanded
                    // Clear selection to avoid interaction issues.
                    list.selectionModel.clearSelection()
                }
            }

            val toolbar = ActionToolbarImpl("AssetSection", DefaultActionGroup(expandAction), true).apply {
                layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
            }
            toolbar.targetComponent = this@apply

            add(headerNameLabel, BorderLayout.WEST)
            add(toolbar.component, BorderLayout.EAST)
            border = SECTION_HEADER_BORDER
        }

        private fun buildName(size: Int?): String {
            val itemNumber = size?.let { " ($it)" } ?: ""
            return "${this@AssetSection.name}$itemNumber"
        }
    }

    override fun getData(p0: String): Any? {
//        viewModel.getData(dataId, getSelectedAssets())
        return null
    }

    override fun dispose() {
        populateResourcesFuture?.cancel(true)
        searchFuture?.cancel(true)
        showLoadingFuture?.cancel(true)
    }
}