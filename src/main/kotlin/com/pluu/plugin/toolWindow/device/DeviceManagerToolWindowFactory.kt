package com.pluu.plugin.toolWindow.device

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

@Suppress("IncorrectServiceRetrieving")
class DeviceManagerToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = "Pluu Device Manager"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance()
            .createContent(DeviceManagerExplorer(project), null, false)
        toolWindow.contentManager.addContent(content)
    }
}