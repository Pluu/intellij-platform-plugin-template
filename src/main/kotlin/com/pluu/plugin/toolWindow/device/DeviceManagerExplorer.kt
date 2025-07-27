package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceProvisioner
import com.android.tools.idea.concurrency.createCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.device.controller.EmulatorUiSettingsController
import com.pluu.plugin.toolWindow.device.tracker.DeviceComboBoxDeviceTracker
import com.pluu.plugin.toolWindow.device.tracker.DeviceEvent.Added
import com.pluu.plugin.toolWindow.device.tracker.DeviceEvent.StateChanged
import com.pluu.plugin.toolWindow.device.tracker.IDeviceComboBoxDeviceTracker
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class DeviceManagerExplorer(
    val project: Project,
    deviceProvisioner: DeviceProvisioner
) : JPanel(), Disposable {

    private val deviceTracker: IDeviceComboBoxDeviceTracker =
        DeviceComboBoxDeviceTracker(deviceProvisioner)

    private val coroutineScope = createCoroutineScope()
    private val emulators = mutableMapOf<Device, EmulatorUiSettingsController>()
    private var latestDevice: Device? = null

    private val deviceComboBox = DeviceComboBox()
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

        coroutineScope.launch(Dispatchers.Main) {
            trackSelected().collect { item ->
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

    private fun trackSelected(): Flow<Device> = callbackFlow {
        // If an item is already selected, the listener will not send it, so we send it now
        (deviceComboBox.selectedItem as? Device)?.let { trySendBlocking(it) }
        val listener = ActionListener { deviceComboBox.item?.let { trySendBlocking(it) } }
        deviceComboBox.addActionListener(listener)
        launch {
            deviceTracker.trackDevices().collect {
                when (it) {
                    is Added -> deviceAdded(it.device)
                    is StateChanged -> {
                        if (it.device.isOnline) {
                            deviceStateChanged(it.device)
                        } else {
                            deviceRemove(it.device)
                            settingsPanel.bind(null)
                        }
                    }
                }
            }
            this@callbackFlow.close()
        }
        awaitClose { deviceComboBox.removeActionListener(listener) }
    }

    private fun deviceAdded(device: Device) {
        if (deviceComboBox.containsDevice(device)) {
            deviceStateChanged(device)
        } else {
            val item = deviceComboBox.addDevice(device)
            when {
                deviceComboBox.selectedItem != null -> return
                else -> selectItem(item)
            }
        }
    }

    private fun deviceRemove(device: Device) {
        deviceComboBox.removeDevice(device)
    }

    private fun selectItem(item: Device?) {
        deviceComboBox.selectedItem = item
    }

    private fun deviceStateChanged(device: Device) {
        when (deviceComboBox.containsDevice(device)) {
            true ->
                deviceComboBox.replaceDevice(
                    device,
                    device.deviceId == deviceComboBox.item.deviceId,
                )

            false -> deviceAdded(device) // Device was removed manually so we re-add it
        }
    }

    override fun dispose() {

    }
}
