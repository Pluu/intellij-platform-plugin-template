@file:Suppress("DialogTitleCapitalization")

package com.pluu.plugin.toolWindow.designsystem.importer

import com.android.tools.idea.ui.resourcemanager.widget.ChessBoardPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.SideBorder
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.settings.ConfigSettings
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.StartupUiUtil
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jdesktop.swingx.prompt.PromptSupport
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent

private val DIALOG_SIZE = JBUI.size(1000, 700)
private val PREVIEW_SIZE = JBUI.size(150)

private val CONTENT_PANEL_BORDER = JBUI.Borders.empty(0, 8)

class ResourceImportDialog(
    project: Project,
    private val dialogViewModel: ResourceImportDialogViewModel
) : DialogWrapper(project) {

    private val assetSetToView = IdentityHashMap<DesignAssetSet, DesignAssetSetView>()

    private lateinit var fileCountLabel: JLabel

    private lateinit var content: JPanel

    private val centerPanel = panel {
        row {
            fileCountLabel = label("").component
            cell(createImportButtonAction())
                .align(AlignX.RIGHT)
        }
        row {
            content = JPanel(VerticalLayout(0)).apply {
                border = CONTENT_PANEL_BORDER
            }

            val centerScroll = JBScrollPane(content).apply {
                preferredSize = DIALOG_SIZE
                border = null
            }
            cell(centerScroll)
        }
    }.withBorder(
        BorderFactory.createCompoundBorder(
            IdeBorderFactory.createBorder(SideBorder.BOTTOM),
            CONTENT_PANEL_BORDER
        )
    )

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
                    dialogViewModel.importMoreAssetIfEmpty { resourceSet ->
                        addAssets(resourceSet)
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

    override fun createCenterPanel(): JComponent = centerPanel

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
        designAssetSet: DesignAssetSet
    ) {
        addDesignAssetSet(designAssetSet)
        updateOkButton()
    }

    private fun createImportButtonAction(): JComponent {
        val importAction = object : DumbAwareAction(
            "Import more assets",
            "Import more assets",
            AllIcons.Actions.Upload
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                dialogViewModel.importMoreAssets { designAssetSet ->
                    addAssets(designAssetSet)
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
        content.scrollRectToVisible(
            Rectangle(content.width, content.height, content.width + 1, content.height + 1)
        )
    }

    private fun updateOkButton() {
        isOKActionEnabled = assetSetToView.isNotEmpty() && assetSetToView.all {
            it.key.isValidate()
        }
    }

    override fun doOKAction() {
        super.doOKAction()
        dialogViewModel.commit()
    }

    /**
     * View showing a [DesignAssetSet] and its contained [DesignSystemItem].
     */
    private inner class DesignAssetSetView(
        private var assetSet: DesignAssetSet
    ) : JPanel(BorderLayout()) {

        ///////////////////////////////////////////////////////////////////////////
        // Header Panel
        ///////////////////////////////////////////////////////////////////////////

        private val assetNameField = JBTextField(assetSet.name).apply {
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

        private val assetAliasNameField = JBTextField(assetSet.asset.aliasName).apply {
            this.font = StartupUiUtil.labelFont.deriveFont(JBUI.scaleFontSize(14f))
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    updateAliasName(this@apply.text)
                    ComponentValidator.getInstance(this@apply).ifPresent(ComponentValidator::revalidate)
                }
            })
        }

        private val header = panel {
            row {
                cell(assetNameField)
                    .label("Class name:")
                    .columns(30)
            }
            row {
                cell(assetAliasNameField)
                    .label("Alias name:")
                    .columns(30)
            }
        }

        private val preview = JBLabel().apply {
            horizontalAlignment = JBLabel.CENTER
        }

        private val previewWrapper = ChessBoardPanel().apply {
            preferredSize = PREVIEW_SIZE
            maximumSize = PREVIEW_SIZE
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            add(preview)
        }

        private val sampleCodeTextArea = JTextArea(assetSet.asset.sampleCode).apply {
            setLineWrap(true)
            setWrapStyleWord(true)
            PromptSupport.setPrompt("Input sample code", this)

            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(event: DocumentEvent) {
                    updateSampleCode(this@apply.text)
                }
            })
        }

        private val configurationPanel = panel {
            row("Design system Type:") {
                comboBox(
                    ConfigSettings.getInstance().getTypes(),
                    SimpleListCellRenderer.create("") { it.name }
                ).applyToComponent {
                    selectedItem = null
                }.whenItemSelectedFromUi {
                    updateDesignSystemType(it)
                }
            }
            row {
                label("Sample code:")
                    .applyToComponent { preferredWidth = 150 }
                comboBox(
                    ApplicableFileType.selectableTypes().toList(),
                    SimpleListCellRenderer.create("") { it.name }
                ).label("Applicable file:")
                    .applyToComponent {
                        selectedItem = null
                    }.whenItemSelectedFromUi {
                        updateApplicableFileType(it)
                    }
            }
            row {
                cell(
                    JBScrollPane(sampleCodeTextArea).apply {
                        border = BorderFactory.createCompoundBorder(
                            RoundedLineBorder(UIUtil.getTreeSelectionBackground(true), JBUI.scale(4), JBUI.scale(2)),
                            JBUI.Borders.empty(4)
                        )
                        preferredSize = Dimension(sampleCodeTextArea.preferredSize.width, 80)
                        minimumSize = preferredSize
                        setViewportView(sampleCodeTextArea)
                    }
                ).align(Align.FILL)
            }
        }

        private val middlePane = panel {
            border = JBUI.Borders.empty(4)
            row {
                panel {
                    row {
                        label(assetSet.asset.name)
                        label(StringUtil.formatFileSize(assetSet.asset.file?.length ?: 0))
                    }
                }
                link("Do not import") {
                    removeAsset()
                }.align(AlignX.RIGHT)
            }
            row {
                cell(configurationPanel).align(Align.FILL)
            }
        }

        init {
            add(header, BorderLayout.NORTH)
            add(panel {
                row {
                    cell(previewWrapper).align(AlignY.TOP)
                    cell(middlePane).align(Align.FILL)
                }
            })

            dialogViewModel.getAssetPreview(assetSet.asset).whenComplete { image, _ ->
                image?.let {
                    preview.icon = ImageIcon(it)
                    preview.repaint()
                }
            }
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

        private fun updateAliasName(aliasName: String?) {
            dialogViewModel.updateAliasName(assetSet, aliasName, ::updateNewAssetSet)
        }

        private fun updateNewAssetSet(newDesignAssetSet: DesignAssetSet) {
            val assetSetView = assetSetToView.remove(assetSet) ?: return
            assetSet = newDesignAssetSet
            assetSetToView[newDesignAssetSet] = assetSetView
            updateOkButton()
        }

        private fun removeAsset() {
            dialogViewModel.removeAsset(this.assetSet)
            assetSetToView.remove(this.assetSet, this)
            parent.remove(this)
            content.revalidate()
            content.repaint()
            updateOkButton()
        }
    }
}