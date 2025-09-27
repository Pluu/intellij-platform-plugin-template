package com.pluu.plugin.action

import com.android.tools.idea.device.explorer.DeviceExplorerToolWindowFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class NonWizardAction : AnAction(), DumbAware {

    override fun actionPerformed(p0: AnActionEvent) {
        SampleDialog(p0.project).show()
    }
}

class SampleDialog(
    private val project: Project?
) : DialogWrapper(project) {

    private var openRunConfigWhenDone = true

    init {
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            checkBox("Option label")
                .label("Check")
                .bindSelected(::openRunConfigWhenDone)
        }
    }

    override fun doOKAction() {
        super.doOKAction()
        notify("[OK] Result : $openRunConfigWhenDone")
    }

    override fun doCancelAction() {
        super.doCancelAction()
        notify("[Cancel click]")
    }

    private fun notify(message: String) {
        val notification = Notification(
            DeviceExplorerToolWindowFactory.TOOL_WINDOW_ID,
            "Project opened detected",
            message,
            NotificationType.INFORMATION
        )
        notification.notify(project)
    }
}