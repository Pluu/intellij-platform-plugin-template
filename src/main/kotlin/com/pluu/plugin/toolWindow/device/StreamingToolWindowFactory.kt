///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/core/StreamingToolWindowFactory.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ToolWindowWindowAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowContentUiType
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi

class StreamingToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setTitleActions(listOf(MoveToWindowAction(toolWindow)))
        toolWindow.setDefaultContentUiType(ToolWindowContentUiType.TABBED)
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.component.putClientProperty(ToolWindowContentUi.ALLOW_DND_FOR_TABS, true)
        toolWindow.component.putClientProperty(ToolWindowContentUi.DONT_HIDE_TOOLBAR_IN_HEADER, true)
        DeviceToolWindowManager(toolWindow as ToolWindowEx)
    }

    private class MoveToWindowAction(private val toolWindow: ToolWindow) : ToolWindowWindowAction() {
        override fun update(event: AnActionEvent) {
            when (toolWindow.type) {
                ToolWindowType.FLOATING, ToolWindowType.WINDOWED -> event.presentation.isEnabledAndVisible = false
                else -> {
                    super.update(event)
                    event.presentation.icon = AllIcons.Actions.MoveToWindow
                }
            }
        }
    }
}