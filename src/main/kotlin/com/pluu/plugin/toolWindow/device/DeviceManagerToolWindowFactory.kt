///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/core/StreamingToolWindowFactory.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class DeviceManagerToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun init(toolWindow: ToolWindow) {
        DeviceToolWindowManager(toolWindow)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    }
}