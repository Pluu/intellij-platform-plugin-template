package com.pluu.plugin.toolWindow.designsystem

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/ResourceExplorerToolFactory.kt
///////////////////////////////////////////////////////////////////////////

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
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.settings.ConfigSettings
import com.pluu.plugin.settings.ConfigSettingsListener
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemType
import org.jetbrains.android.facet.AndroidFacet
import javax.swing.JComponent

const val DESIGN_SYSTEM_EXPLORER_TOOL_WINDOW_ID = "Design System Explorer"

class DesignSystemToolWindowFactory : ToolWindowFactory, DumbAware, ToolWindowManagerListener {

    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = "Design System"

        val project = toolWindow.project
        project.messageBus.connect(toolWindow.disposable)
            .subscribe(ConfigSettingsListener.TOPIC, object : ConfigSettingsListener {
                override fun onEnableChanged(isEnable: Boolean) {
                    updateAvailable(toolWindow, isEnable)
                }

                override fun onDesignSystemTypeChanged(list: List<DesignSystemType>) {
                    createContent(project, toolWindow)
                }
            })
    }

    override fun isApplicable(project: Project): Boolean {
        return true
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return ConfigSettings.getInstance().isDesignSystemEnable
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        createContent(project, toolWindow)
    }

    private fun createContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.displayLoading()
        ClearResourceCacheAfterFirstBuild.getInstance(project).runWhenResourceCacheClean(
            onCacheClean = {
                project.runWhenSmartAndSyncedOnEdt(callback = {
                    createContent(toolWindow, project)
                })
            },
            onSourceGenerationError = {
                toolWindow.displayWaitingForGoodSync()
            },
            toolWindow.disposable
        )
        project.messageBus.connect(project)
            .subscribe(ToolWindowManagerListener.TOPIC, MyToolWindowManagerListener(project))
    }
}

private fun updateAvailable(toolWindow: ToolWindow, designSystemEnable: Boolean) {
    toolWindow.isAvailable = designSystemEnable
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
    val waitingForSyncPanel = panel {
        row {
            label(message)
                .applyToComponent {
                    if (showWarning) {
                        icon = AllIcons.General.Warning
                    }
                    foreground = ColorUtil.toAlpha(UIUtil.getLabelForeground(), 150)
                }.align(Align.CENTER)
        }.resizableRow()
    }.apply {
        alignmentX = JComponent.CENTER_ALIGNMENT
        alignmentY = JComponent.CENTER_ALIGNMENT
    }
    val content = contentManager.factory.createContent(waitingForSyncPanel, null, false)
    contentManager.addContent(content)
}

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

private class MyToolWindowManagerListener(
    private val project: Project
) : ToolWindowManagerListener {

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
