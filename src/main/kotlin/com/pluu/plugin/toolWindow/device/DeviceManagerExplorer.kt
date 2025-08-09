package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceProvisioner
import com.android.tools.idea.concurrency.createCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.device.controller.EmulatorUiSettingsController
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class DeviceManagerExplorer(
    val project: Project,
    deviceProvisioner: DeviceProvisioner
) : JPanel(), Disposable {

    private val coroutineScope = createCoroutineScope()
    private val emulators = mutableMapOf<Device, EmulatorUiSettingsController>()
    private var latestDevice: Device? = null

    private val deviceComboBox = DeviceComboBox(deviceProvisioner)
    private val settingsPanel = UiSettingsPanel()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(0, 10, 0, 10)

        add(
            JBLabel("Select device: ").apply {
                foreground = UIUtil.getInactiveTextColor()
                preferredSize = Dimension(Int.MAX_VALUE, preferredSize.height)
                minimumSize = preferredSize
            }
        )
        add(
            deviceComboBox.apply {
                maximumSize = Dimension(maximumSize.width, preferredSize.height)
            }
        )
        add(
            JBScrollPane(
                settingsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            ).apply {
                border = BorderFactory.createEmptyBorder()
                verticalScrollBar.unitIncrement = 10
                verticalScrollBar.preferredSize = Dimension(10, verticalScrollBar.preferredSize.height)
            }
        )

        coroutineScope.launch(Dispatchers.Default) {
            deviceComboBox.trackSelected().collect { item ->
                if (latestDevice == item) return@collect
                latestDevice = item
                updatePanel(item)
            }
        }
    }

    private fun updatePanel(item: Device) {
        settingsPanel.bind(
            item.takeIf { it.isOnline }?.uiSettingsModel
        )

        val newEmulators = emulators.filter {
            it.key.isOnline
        }
        emulators.clear()
        emulators.putAll(newEmulators)

        val controller = emulators.getOrPut(item) {
            EmulatorUiSettingsController(
                project,
                item.serialNumber,
                item.uiSettingsModel,
                this
            )
        }

        coroutineScope.launch(Dispatchers.Main) {
            controller.populateModel()
        }
    }

    override fun dispose() {

    }
}
