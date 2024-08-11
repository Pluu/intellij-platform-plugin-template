package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceType
import com.android.tools.idea.concurrency.AndroidCoroutineScope
import com.android.tools.idea.concurrency.AndroidDispatchers.workerThread
import com.android.tools.idea.streaming.device.SuspendingSocketChannel
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.device.combobox.DeviceComboBox
import com.pluu.plugin.toolWindow.device.controller.device.DeviceController
import com.pluu.plugin.toolWindow.device.controller.device.DeviceUiSettingsController
import com.pluu.plugin.toolWindow.device.controller.emulator.EmulatorUiSettingsController
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsPanel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.nio.channels.AsynchronousSocketChannel
import javax.swing.JPanel

class DeviceManagerExplorer(
    val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val coroutineScope = AndroidCoroutineScope(this)

    private val deviceComboBox = DeviceComboBox(project, null)

    private val toolbarPanel = panel {
        row {
            cell(deviceComboBox).align(Align.FILL)
        }.topGap(TopGap.SMALL)
    }.withBorder(JBUI.Borders.empty())

    private var subPanel: JPanel? = null

    init {
        border = JBUI.Borders.empty(4)

        add(toolbarPanel, BorderLayout.NORTH)
        coroutineScope.launch(workerThread) {
            deviceComboBox.trackSelected().collect { item ->
                updatePanel(item)
            }
        }
    }

    private fun updatePanel(item: Device) {
        if (subPanel != null) {
            remove(subPanel)
        }
        val panel = UiSettingsPanel(item.uiSettingsModel, item.type ?: DeviceType.HANDHELD)
        subPanel = panel

        val controller = if (item.isEmulator) {
            EmulatorUiSettingsController(
                project,
                item.serialNumber,
                item.uiSettingsModel,
                item.deviceInfo,
                this
            )
        } else {
            DeviceUiSettingsController(
                DeviceController(this, SuspendingSocketChannel(AsynchronousSocketChannel.open())),
                item.deviceInfo,
                project,
                item.uiSettingsModel,
                this
            )
        }
        AndroidCoroutineScope(this).launch {
            controller.populateModel()
            add(panel)
        }
    }

    override fun dispose() {

    }
}
