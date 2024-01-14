package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.help.AndroidWebHelpProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.ide.wizard.Step
import com.intellij.ide.wizard.StepAdapter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.StartupUiUtil
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.KeyboardFocusManager
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyChangeListener
import java.util.*
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

private const val DIALOG_TITLE = "Import component"

private val DIALOG_SIZE = JBUI.size(1000, 700)

private val ASSET_GROUP_BORDER = BorderFactory.createCompoundBorder(
    JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
    JBUI.Borders.empty(12, 0, 8, 0)
)

private val NORTH_PANEL_BORDER = BorderFactory.createCompoundBorder(
    JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
    JBUI.Borders.empty(16)
)

private val CONTENT_PANEL_BORDER = JBUI.Borders.empty(0, 20)

class ResourceImportDialog(
    private val dialogViewModel: ResourceImportDialogViewModel
) : AbstractWizard<Step>(DIALOG_TITLE, dialogViewModel.facet.module.project) {

    private val assetSetToView = IdentityHashMap<DesignAssetSet, DesignAssetSetView>()

    private val content = JPanel(VerticalLayout(0)).apply {
        border = CONTENT_PANEL_BORDER
    }

    val root = JBScrollPane(content).apply {
        preferredSize = DIALOG_SIZE
        border = null
    }

    private val fileCountLabel = JBLabel()

    private val northPanel = JPanel(BorderLayout()).apply {
        border = NORTH_PANEL_BORDER
        add(fileCountLabel, BorderLayout.WEST)
        add(createImportButtonAction(), BorderLayout.EAST)
    }

    private val focusPropertyChangeListener = PropertyChangeListener { evt ->
        if (evt.newValue !is JComponent) {
            return@PropertyChangeListener
        }
        val focused: JComponent = evt.newValue as JComponent
//        scrollViewPortIfNeeded(focused)
    }

    init {
        addWizardSteps()
        setSize(DIALOG_SIZE.width(), DIALOG_SIZE.height())
        isResizable = false
        dialogViewModel.updateCallback = ::updateValues
        init()
        dialogViewModel.assetSets.forEach(this::addDesignAssetSet)
        updateValues()
        setupWindowListener()
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addPropertyChangeListener("focusOwner", focusPropertyChangeListener)
    }

    /**
     * Setup a window listener that will display the file picker as soon as the dialog
     * opens if the [ResourceImportDialogViewModel] contains no asset.
     */
    private fun setupWindowListener() {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            return
        }
        window.addWindowListener(object : WindowAdapter() {
            // Check to avoid opening twice the asset manager when windowActivated is called twice
            // to fix b/254689088
            private var isWindowActive = false

            // Sometimes windowActivated is triggered twice from the window listener.
            // It seems a race condition issue from java.awt.Window
            // when the synchronized addWindowListener is called.
            override fun windowActivated(e: WindowEvent?) {
                super.windowActivated(e)
                if (!isWindowActive) {
                    isWindowActive = true
                    dialogViewModel.importMoreAssetIfEmpty { resourceSet, designAssets ->
                        this@ResourceImportDialog.addAssets(resourceSet, designAssets)
                    }
                }
            }

            override fun windowClosed(e: WindowEvent?) {
                // Remove the listener when the window is closed otherwise it will be displayed
                // each time the dialog has the focus.
                e?.window?.removeWindowListener(this)
                isWindowActive = false
                super.windowClosed(e)
            }
        })
    }

    private fun addWizardSteps() {
        addStep(object : StepAdapter() {
            override fun _commit(finishChosen: Boolean) {
                if (doValidateAll().isEmpty()) {
                    dialogViewModel.commit()
                }
            }

            override fun getComponent() = root
        })
    }

    override fun createNorthPanel() = northPanel

    private fun updateValues() {
        val importedAssetCount = dialogViewModel.fileCount
        fileCountLabel.text =
            "$importedAssetCount ${StringUtil.pluralize("component", importedAssetCount)} ready to be imported"
    }

    private fun addDesignAssetSet(assetSet: DesignAssetSet) {
        val view = DesignAssetSetView(assetSet)
        content.add(view)
        assetSetToView[assetSet] = view
    }

    /**
     * If a [DesignAssetSetView] already exists for [designAssetSet], merge the [newDesignAssets]
     * within this view, otherwise create a new [DesignAssetSetView].
     */
    private fun addAssets(
        designAssetSet: DesignAssetSet,
        newDesignAssets: DesignSystemItem
    ) {
        val existingView = assetSetToView[designAssetSet]
        if (existingView != null) {
            existingView.addAssetView(newDesignAssets)
        } else {
            addDesignAssetSet(designAssetSet)
        }
        updateStep()
    }

    private fun createImportButtonAction(): JComponent {
        val importAction = object : DumbAwareAction(
            "Import more assets",
            "Import more assets",
            AllIcons.Actions.Upload
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                dialogViewModel.importMoreAssets { designAssetSet, newDesignAssets ->
                    addAssets(designAssetSet, newDesignAssets)
                }
            }
        }

        val presentation = importAction.templatePresentation.clone()
        presentation.text = "Import more files"
        return ActionButtonWithText(
            importAction,
            presentation,
            "Resource Explorer",
            JBUI.size(25)
        ).apply { isFocusable = true }
    }

    /**
     * View showing a [DesignAssetSet] and its contained [DesignSystemItem].
     */
    private inner class DesignAssetSetView(
        private var assetSet: DesignAssetSet
    ) : JPanel(BorderLayout(0, 0)) {

        val assetNameLabel = JBTextField(assetSet.name, 30).apply {
            this.font = StartupUiUtil.labelFont.deriveFont(JBUI.scaleFontSize(14f))
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    performRename(this@apply.text)
                    ComponentValidator.getInstance(this@apply).ifPresent(ComponentValidator::revalidate)
                }
            })

            ComponentValidator(disposable).withValidator { ->
                dialogViewModel.validateName(this.text, this).also {
                    updateButtons()
                }
            }.installOn(this)
                .revalidate()
        }

        val fileViewContainer = JPanel(VerticalFlowLayout(true, false)).apply {
            add(singleAssetView(assetSet.asset))
        }

        private val header = JPanel(BorderLayout()).apply {
            border = ASSET_GROUP_BORDER
            add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                (layout as FlowLayout).alignOnBaseline = true
                add(JBLabel("Class name:"))
                add(assetNameLabel)
            }, BorderLayout.NORTH)
            add(
                FileConfigurationPanel(
                    dialogViewModel.fileConfigurationViewModel
                ), BorderLayout.SOUTH
            )
        }

        init {
            add(header, BorderLayout.NORTH)
            add(fileViewContainer)

            dialogViewModel.fileConfigurationViewModel.onConfigurationUpdated = this::updateAliasName
        }

        fun addAssetView(asset: DesignSystemItem) {
            fileViewContainer.add(singleAssetView(asset))
        }

        private fun singleAssetView(asset: DesignSystemItem): FileImportRow {
            val viewModel = dialogViewModel.createFileViewModel(
                asset,
                updateDesignSystemTypeCallback = this::updateDesignSystemType,
                updateSampleCodeCallback = this::updateSampleCode,
                removeCallback = this::removeAsset,
            )
            val fileImportRow = FileImportRow(viewModel)
            dialogViewModel.getAssetPreview(asset).whenComplete { image, _ ->
                image?.let {
                    fileImportRow.preview.icon = ImageIcon(it)
                    fileImportRow.preview.repaint()
                }
            }
            return fileImportRow
        }

        private fun updateDesignSystemType(designSystemType: DesignSystemType) {
            dialogViewModel.updateDesignSystemType(assetSet, designSystemType, ::updateNewAssetSet)
        }

        private fun updateSampleCode(sampleCode: String) {
            dialogViewModel.updateSampleCode(assetSet, sampleCode, ::updateNewAssetSet)
        }

        private fun performRename(assetName: String) {
            dialogViewModel.updateName(assetSet, assetName, ::updateNewAssetSet)
        }

        private fun updateAliasName(aliasName: List<String>) {
            dialogViewModel.updateAliasName(assetSet, aliasName, ::updateNewAssetSet)
        }

        private fun updateNewAssetSet(newDesignAssetSet: DesignAssetSet) {
            val assetSetView = assetSetToView.remove(assetSet)!!
            assetSet = newDesignAssetSet
            assetSetToView[newDesignAssetSet] = assetSetView
            updateButtons()
        }

        private fun removeAsset() {
            dialogViewModel.removeAsset(this.assetSet)
            if (fileViewContainer.componentCount == 0) {
                assetSetToView.remove(this.assetSet, this)
                parent.remove(this)
                root.revalidate()
                root.repaint()
                updateStep()
            }
        }
    }

    override fun canFinish(): Boolean {
        return assetSetToView.isNotEmpty() && assetSetToView.all {
            it.key.isValidate()
        }
    }

    override fun getHelpID(): String {
        return AndroidWebHelpProvider.HELP_PREFIX + "studio/write/design-system-manager"
    }
}