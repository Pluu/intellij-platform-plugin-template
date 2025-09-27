package com.pluu.plugin.action

import com.android.tools.idea.device.explorer.DeviceExplorerToolWindowFactory
import com.android.tools.idea.wizard.model.ModelWizard
import com.android.tools.idea.wizard.model.ModelWizardStep
import com.android.tools.idea.wizard.ui.StudioWizardDialogBuilder
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.actionListener
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class WizardStepAction : AnAction(), DumbAware {

    override fun actionPerformed(p0: AnActionEvent) {
        val wizard = ModelWizard.Builder()
            .addStep(AehdInstallInfoStep(p0.project))
            .build()
        StudioWizardDialogBuilder(wizard, ">>>")
            .setProject(p0.project)
            .build()
            .show()
    }
}

class AehdInstallInfoStep(
    private val project: Project?
) : ModelWizardStep.WithoutModel("Emulator Settings") {

    private var isChecked = true

    private val panel = panel {
        row {
            label(
                "<html>This wizard will execute Android Emulator hypervisor driver stand-alone installer."
                        + " This is an additional step required to install this package.</html>"
            )
        }
        row {
            checkBox("Option label")
                .label("Check")
                .bindSelected(::isChecked)
                .actionListener {_, it ->
                    isChecked = it.isSelected
                }
        }
    }

    override fun getComponent(): JComponent = panel

    override fun onWizardFinished() {
        super.onWizardFinished()
        notify("[OK] Result : $isChecked")
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
