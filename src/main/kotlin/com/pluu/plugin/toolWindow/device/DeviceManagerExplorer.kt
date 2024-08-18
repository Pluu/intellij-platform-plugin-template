package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceProvisioner
import com.android.tools.idea.concurrency.AndroidCoroutineScope
import com.android.tools.idea.concurrency.AndroidDispatchers.workerThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.device.controller.EmulatorUiSettingsController
import com.pluu.plugin.toolWindow.device.tracker.DeviceComboBoxDeviceTracker
import com.pluu.plugin.toolWindow.device.tracker.DeviceEvent.Added
import com.pluu.plugin.toolWindow.device.tracker.DeviceEvent.StateChanged
import com.pluu.plugin.toolWindow.device.tracker.IDeviceComboBoxDeviceTracker
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsPanel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.ActionListener
import javax.swing.JPanel

class DeviceManagerExplorer(
    val project: Project,
    deviceProvisioner: DeviceProvisioner
) : JPanel(BorderLayout()), Disposable {

    private val deviceTracker: IDeviceComboBoxDeviceTracker =
        DeviceComboBoxDeviceTracker(deviceProvisioner)

    private val coroutineScope = AndroidCoroutineScope(this)
    private val emulators = mutableMapOf<Device, EmulatorUiSettingsController>()

    private val deviceComboBox = DeviceComboBox()
    private val settingsPanel = UiSettingsPanel()

    private val root = panel {
        border = JBUI.Borders.empty(0, 10)
        row {
            label("Select device: ")
                .applyToComponent {
                    foreground = UIUtil.getInactiveTextColor()
                }
        }
        row { cell(deviceComboBox).align(AlignX.FILL) }
        row { cell(settingsPanel).align(AlignX.FILL) }
    }

    init {
        add(root)
        coroutineScope.launch(workerThread) {
            trackSelected().collect { item ->
                updatePanel(item)
            }
        }
    }

    private fun updatePanel(item: Device) {
        settingsPanel.bind(
            item.takeIf { it.isDeviceOnline }
                ?.uiSettingsModel
        )

        val newEmulators = emulators.filter {
            it.key.isDeviceOnline
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

        AndroidCoroutineScope(this).launch {
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
                        if (it.device.isDeviceOnline) {
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
