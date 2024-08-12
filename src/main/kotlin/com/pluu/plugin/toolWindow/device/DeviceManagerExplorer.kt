package com.pluu.plugin.toolWindow.device

import com.android.tools.idea.concurrency.AndroidCoroutineScope
import com.android.tools.idea.concurrency.AndroidDispatchers.workerThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.pluu.plugin.toolWindow.device.combobox.DeviceComboBox
import com.pluu.plugin.toolWindow.device.controller.emulator.EmulatorUiSettingsController
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsPanel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel

class DeviceManagerExplorer(
    val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val coroutineScope = AndroidCoroutineScope(this)
    private val emulators = mutableMapOf<String, EmulatorUiSettingsController>()

    private val deviceComboBox = DeviceComboBox(project, null)
    private val settingsPanel = UiSettingsPanel()

    private val toolbarPanel = panel {
        row {
            cell(deviceComboBox).align(Align.FILL)
        }.topGap(TopGap.SMALL)
    }.withBorder(JBUI.Borders.empty())

    init {
        border = JBUI.Borders.empty(4)

        add(toolbarPanel, BorderLayout.NORTH)
        add(settingsPanel)

        coroutineScope.launch(workerThread) {
            deviceComboBox.trackSelected().collect { item ->
                updatePanel(item)
            }
        }
    }

    private fun updatePanel(item: Device) {
        settingsPanel.bind(item.uiSettingsModel)

        val controller = emulators.getOrPut(item.serialNumber) {
            EmulatorUiSettingsController(
                project,
                item.serialNumber,
                item.uiSettingsModel,
                item.deviceInfo,
                this
            )
        }

        AndroidCoroutineScope(this).launch {
            controller.populateModel()
        }
    }

    override fun dispose() {

    }
}
