@file:Suppress("DialogTitleCapitalization")

package com.pluu.plugin.toolWindow.designsystem.importer

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.StartupUiUtil
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

private val DIALOG_SIZE = JBUI.size(1000, 700)

private val CONTENT_PANEL_BORDER = JBUI.Borders.empty(0, 8)

class ResourceImportDialog(
    project: Project,
    private val dialogViewModel: ResourceImportDialogViewModel
) : DialogWrapper(project) {

    private val assetSetToView = IdentityHashMap<DesignAssetSet, DesignAssetSetView>()

    private lateinit var fileCountLabel: JLabel

    private val northPanel = panel {
        row {
            fileCountLabel = label("").component
            cell(createImportButtonAction())
                .align(AlignX.RIGHT)
                .enabled(dialogViewModel.isAddMode)
        }
    }.withBorder(
        BorderFactory.createCompoundBorder(
            IdeBorderFactory.createBorder(SideBorder.BOTTOM),
            CONTENT_PANEL_BORDER
        )
    )

    private val content = JPanel(VerticalLayout(0)).apply {
        border = CONTENT_PANEL_BORDER
    }

    private val centerPanel = JBScrollPane(content).apply {
        preferredSize = DIALOG_SIZE
        border = null
    }

    init {
        title = "Import Component"
        setSize(DIALOG_SIZE.width(), DIALOG_SIZE.height())
        isResizable = false
        dialogViewModel.updateCallback = ::updateValues
        init()
        // 최초 데이터 추가
        dialogViewModel.assetSets.forEach(this::addDesignAssetSet)
        updateValues()
        setupWindowListener()
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
                        addAssets(resourceSet, designAssets)
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

    override fun createNorthPanel(): DialogPanel = northPanel

    override fun createCenterPanel(): JComponent = centerPanel

    private fun updateValues() {
        val importedAssetCount = dialogViewModel.fileCount
        fileCountLabel.text =
            "$importedAssetCount ${StringUtil.pluralize("component", importedAssetCount)} ready to be imported"
    }

    private fun addDesignAssetSet(assetSet: DesignAssetSet) {
        val view = DesignAssetSetView(assetSet, dialogViewModel.isAddMode)
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
        updateOkButton()
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
                    jumpToImportStep()
                }
            }
        }

        return ActionButtonWithText(
            importAction,
            importAction.templatePresentation.clone(),
            "",
            JBUI.size(25)
        )
    }

    /** Import 후 추가된 항목이 보이도록 끝부분을 스크롤 */
    private fun jumpToImportStep() {
        centerPanel.viewport.scrollRectToVisible(
            Rectangle(content.width, content.height, content.width + 1, content.height + 1)
        )
    }

    private fun updateOkButton() {
        isOKActionEnabled = assetSetToView.isNotEmpty() && doValidateAll().isEmpty()
    }

    override fun doValidate(): ValidationInfo? {
        val result = dialogViewModel.getValidationInfo()
        setErrorInfoAll(if (result != null) listOf(result) else emptyList())
        return result
    }

    override fun doOKAction() {
        super.doOKAction()
        dialogViewModel.commit()
    }

    /**
     * View showing a [DesignAssetSet] and its contained [DesignSystemItem].
     */
    private inner class DesignAssetSetView(
        private var assetSet: DesignAssetSet,
        private val isAddMode: Boolean
    ) : JPanel(BorderLayout()) {

        val assetNameField = JBTextField(assetSet.name).apply {
            this.font = StartupUiUtil.labelFont.deriveFont(JBUI.scaleFontSize(14f))
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    performRename(this@apply.text)
                    ComponentValidator.getInstance(this@apply).ifPresent(ComponentValidator::revalidate)
                }
            })

            ComponentValidator(disposable).withValidator { ->
                dialogViewModel.validateName(assetSet.asset.type, this.text, this).also {
                    updateOkButton()
                }
            }.installOn(this)
                .revalidate()
        }

        val fileViewContainer = JPanel(VerticalFlowLayout(true, false)).apply {
            add(singleAssetView(assetSet.asset))
        }

        private val header = panel {
            row {
                cell(assetNameField)
                    .label("Class name:")
                    .columns(30)
            }
            row {
                cell(FileConfigurationPanel(dialogViewModel.fileConfigurationViewModel))
            }
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
                updateApplicableFileTypeCallback = this::updateApplicableFileType,
                removeCallback = this::removeAsset,
            )
            val fileImportRow = FileImportRow(viewModel, isAddMode)
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

        private fun updateApplicableFileType(type: ApplicableFileType) {
            dialogViewModel.updateApplicableFileType(assetSet, type, ::updateNewAssetSet)
        }

        private fun performRename(assetName: String) {
            dialogViewModel.updateName(assetSet, assetName, ::updateNewAssetSet)
        }

        private fun updateAliasName(aliasName: List<String>) {
            dialogViewModel.updateAliasName(assetSet, aliasName, ::updateNewAssetSet)
        }

        private fun updateNewAssetSet(newDesignAssetSet: DesignAssetSet) {
            val assetSetView = assetSetToView.remove(assetSet) ?: return
            assetSet = newDesignAssetSet
            assetSetToView[newDesignAssetSet] = assetSetView
            ComponentValidator.getInstance(assetNameField).ifPresent(ComponentValidator::revalidate)
        }

        private fun removeAsset() {
            dialogViewModel.removeAsset(this.assetSet)
            if (fileViewContainer.componentCount == 0) {
                assetSetToView.remove(this.assetSet, this)
                parent.remove(this)
                centerPanel.revalidate()
                centerPanel.repaint()
                updateOkButton()
            }
        }
    }
}