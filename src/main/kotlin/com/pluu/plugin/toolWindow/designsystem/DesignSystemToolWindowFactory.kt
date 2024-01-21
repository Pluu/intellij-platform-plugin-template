package com.pluu.plugin.toolWindow.designsystem

import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.startup.ClearResourceCacheAfterFirstBuild
import com.android.tools.idea.ui.resourcemanager.explorer.NoFacetView
import com.android.tools.idea.util.listenUntilNextSync
import com.android.tools.idea.util.runWhenSmartAndSyncedOnEdt
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import org.jetbrains.android.facet.AndroidFacet
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

const val DESIGN_SYSTEM_EXPLORER_TOOL_WINDOW_ID = "Design System Explorer"

class DesignSystemToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = "Design System"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.displayLoading()
        ClearResourceCacheAfterFirstBuild.getInstance(project).runWhenResourceCacheClean(
            onCacheClean = {
                project.runWhenSmartAndSyncedOnEdt(callback = {
                    createContent(toolWindow, project)
                })
            },
            onSourceGenerationError = {
                toolWindow.displayWaitingForGoodSync()
            }
        )
        project.messageBus.connect(project)
            .subscribe(ToolWindowManagerListener.TOPIC, MyToolWindowManagerListener(project))
    }
}

private fun connectListeners(
    toolWindow: ToolWindow,
    project: Project,
    designSystemExplorer: DesignSystemExplorer
) {
    val connection = project.messageBus.connect(designSystemExplorer)
    connection.subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        MyFileEditorListener(project, toolWindow, designSystemExplorer)
    )
    connection.subscribe(
        PROJECT_SYSTEM_SYNC_TOPIC,
        SyncResultListener(project, designSystemExplorer, toolWindow)
    )
}

private fun createContent(toolWindow: ToolWindow, project: Project) {
    toolWindow.contentManager.removeAllContents(true)
    val facet = findLastSelectedFacet(project) ?: findCompatibleFacetFromOpenedFiles(project)
    if (facet == null) {
        displayNoFacetView(project, toolWindow)
        return
    }

    toolWindow.displayWaitingForGoodSync()

    // No existing successful sync, since there's a fair chance of having rendering issues, wait for next successful sync.
    project.runWhenSmartAndSyncedOnEdt(callback = { result ->
        if (result.isSuccessful) {
            displayInToolWindow(facet, toolWindow)
        } else {
            project.listenUntilNextSync(listener = {
                createContent(toolWindow, project)
            })
        }
    })
}

/**
 * Display the [NoFacetView]. Contains a message and a couple of action links to sync or add an Android Module.
 */
private fun displayNoFacetView(project: Project, toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    val content = contentManager.factory.createContent(NoFacetView(project), null, false)
    contentManager.addContent(content)
}

private fun ToolWindow.displayWaitingView(message: String, showWarning: Boolean) {
    contentManager.removeAllContents(true)
    val waitingForSyncPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val waitingLabel = JBLabel().apply {
            text = message
            if (showWarning) {
                icon = AllIcons.General.Warning
            }
            foreground = ColorUtil.toAlpha(UIUtil.getLabelForeground(), 150)
            alignmentX = JComponent.CENTER_ALIGNMENT
            alignmentY = JComponent.CENTER_ALIGNMENT
        }
        add(Box.createVerticalGlue())
        add(waitingLabel)
        add(Box.createVerticalGlue())
    }
    val content = contentManager.factory.createContent(waitingForSyncPanel, null, false)
    contentManager.addContent(content)
}

private fun ToolWindow.displayWaitingForBuild() = displayWaitingView("Waiting for build to finish...", false)

private fun ToolWindow.displayWaitingForGoodSync() = displayWaitingView("Waiting for successful sync...", true)

private fun ToolWindow.displayLoading() = displayWaitingView("Loading...", false)

/**
 * Display the [DesignSystemExplorer] in the [ToolWindow].
 */
private fun displayInToolWindow(facet: AndroidFacet, toolWindow: ToolWindow) {
    val designSystemExplorer = DesignSystemExplorer.createForToolWindow(facet)
    val contentManager = toolWindow.contentManager
    contentManager.removeAllContents(true)
    val content = contentManager.factory.createContent(designSystemExplorer, null, false)
    Disposer.register(content, designSystemExplorer)
    contentManager.addContent(content)
    content.preferredFocusableComponent = designSystemExplorer
    connectListeners(toolWindow, facet.module.project, designSystemExplorer)
    // ResourceManagerTracking.logPanelOpens(facet)
}

private class MyFileEditorListener(
    val project: Project,
    val toolWindow: ToolWindow,
    val designSystemExplorer: DesignSystemExplorer?
) : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val editor = event.newEditor ?: return
        editorFocused(editor, project, designSystemExplorer)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        editorFocused(source.getSelectedEditor(file) ?: return, project, designSystemExplorer)
    }

    private fun editorFocused(
        editor: FileEditor,
        project: Project,
        designSystemExplorer: DesignSystemExplorer?
    ) {
        val module = editor.file?.let {
            ModuleUtilCore.findModuleForFile(it, project)
        } ?: return

        toolWindow.contentManager.getContent(0)?.displayName = module.name
        val facet = AndroidFacet.getInstance(module)
        if (facet != null && facet != designSystemExplorer?.facet) {
            designSystemExplorer?.facet = facet
        }
    }
}

private class MyToolWindowManagerListener(private val project: Project) : ToolWindowManagerListener {

    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        val window: ToolWindow = toolWindowManager.getToolWindow(DESIGN_SYSTEM_EXPLORER_TOOL_WINDOW_ID) ?: return
        val contentManager = window.contentManager
        val designSystemExplorerIsPresent = contentManager.contents.any { it.component is DesignSystemExplorer }
        if (!window.isVisible) {
            contentManager.removeAllContents(true)
            // ResourceManagerTracking.logPanelCloses()
        } else if (!designSystemExplorerIsPresent) {
            createContent(window, project)
        }
    }
}

private class SyncResultListener(
    private val project: Project,
    private val designSystemExplorer: DesignSystemExplorer,
    private val toolWindow: ToolWindow
) : ProjectSystemSyncManager.SyncResultListener {
    override fun syncEnded(result: ProjectSystemSyncManager.SyncResult) {
        // After sync, if the facet is not found anymore, recreate the view.
        if (!compatibleFacetExists(designSystemExplorer.facet)) {
            createContent(toolWindow, project)
        }
    }
}
