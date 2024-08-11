///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceComboBox.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.combobox

import com.android.sdklib.deviceprovisioner.DeviceIcons
import com.android.tools.idea.deviceprovisioner.StudioDefaultDeviceIcons
import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.util.ui.components.BorderLayoutPanel
import com.pluu.plugin.toolWindow.device.Device
import com.pluu.plugin.toolWindow.device.LogcatBundle
import com.pluu.plugin.toolWindow.device.combobox.DeviceEvent.Added
import com.pluu.plugin.toolWindow.device.combobox.DeviceEvent.StateChanged
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_CAR
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_PHONE
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_TV
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_WEAR
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.event.ActionListener
import javax.swing.JList

private val PHYSICAL_ICONS = StudioDefaultDeviceIcons
private val EMULATOR_ICONS =
    DeviceIcons(VIRTUAL_DEVICE_PHONE, VIRTUAL_DEVICE_WEAR, VIRTUAL_DEVICE_TV, VIRTUAL_DEVICE_CAR)

internal class DeviceComboBox(
    project: Project,
    private val initialItem: Device?,
) : ComboBox<Device>() {
    private val deviceTracker: IDeviceComboBoxDeviceTracker =
        project
            .service<DeviceComboBoxDeviceTrackerFactory>()
            .createDeviceComboBoxDeviceTracker(initialItem)

    private val deviceComboModel: DeviceComboModel
        get() = model as DeviceComboModel

    init {
        renderer = DeviceComboBoxRenderer()
        model = DeviceComboModel()
    }

    override fun updateUI() {
        setUI(DeviceComboBoxUi())
    }

    fun trackSelected(): Flow<Device> = callbackFlow {
        // If an item is already selected, the listener will not send it, so we send it now
        (selectedItem as? Device)?.let { trySendBlocking(it) }
        val listener = ActionListener { item?.let { trySendBlocking(it) } }
        addActionListener(listener)
        launch {
            deviceTracker.trackDevices().collect {
                when (it) {
                    is Added -> deviceAdded(it.device)
                    is StateChanged -> deviceStateChanged(it.device)
                }
            }
            this@callbackFlow.close()
        }
        awaitClose { removeActionListener(listener) }
    }

    private fun deviceAdded(device: Device) {
        if (deviceComboModel.containsDevice(device)) {
            deviceStateChanged(device)
        } else {
            val item = deviceComboModel.addDevice(device)
            when {
                selectedItem != null -> return
                initialItem == null -> selectItem(item)
                device.deviceId == initialItem.deviceId -> selectItem(item)
            }
        }
    }

    private fun selectItem(item: Device) {
        selectedItem = item
    }

    private fun deviceStateChanged(device: Device) {
        when (deviceComboModel.containsDevice(device)) {
            true ->
                if (device.isOnline) {
                    deviceComboModel.replaceDevice(
                        device,
                        device.deviceId == item.deviceId,
                    )
                } else {
                    deviceComboModel.removeDevice(device)
                }

            false -> deviceAdded(device) // Device was removed manually so we re-add it
        }
    }

    private class DeviceComboBoxRenderer : ColoredListCellRenderer<Device>() {
        private val component = BorderLayoutPanel()

        init {
            component.isOpaque = false
        }

        override fun getListCellRendererComponent(
            list: JList<out Device>,
            value: Device?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ): Component {
            val deviceComponent =
                super.getListCellRendererComponent(list, value, index, selected, hasFocus)
            component.addToLeft(deviceComponent)
            return component
        }

        override fun customizeCellRenderer(
            list: JList<out Device>,
            item: Device?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            if (item == null) {
                return
            }
            renderDevice(item)
        }

        private fun renderDevice(device: Device) {
            val icons = if (device.isEmulator) EMULATOR_ICONS else PHYSICAL_ICONS
            icon = icons.iconForDeviceType(device.type)

            append(device.name, REGULAR_ATTRIBUTES)
            if (device.isOnline) {
                append(" (${device.serialNumber})", REGULAR_ATTRIBUTES)
            } else {
                append(LogcatBundle.message("logcat.device.combo.offline"), GRAYED_BOLD_ATTRIBUTES)
            }
        }
    }

    private class DeviceComboModel : CollectionComboBoxModel<Device>() {

        fun addDevice(device: Device): Device {
            add(device)
            return device
        }

        fun replaceDevice(device: Device, setSelected: Boolean) {
            val index = items.indexOfFirst { it.deviceId == device.deviceId }
            if (index < 0) {
//                LOGGER.warn("Device ${device.deviceId} expected to exist but was not found")
                return
            }
            setElementAt(device, index)
            if (setSelected) {
                selectedItem = device
            }
        }

        fun removeDevice(device: Device) {
            val index = items.indexOfFirst { it.deviceId == device.deviceId }
            remove(index)
            (selectedItem as? Device)?.let {
                if (it.serialNumber == device.serialNumber) {
                    selectedItem = null
                }
            }
        }

        fun containsDevice(device: Device): Boolean =
            items.find { it.deviceId == device.deviceId } != null
    }

    /**
     * A custom UI based on DarculaComboBoxUI that has more control over the popup, so we can
     * intercept mouse events.
     */
    private class DeviceComboBoxUi : DarculaComboBoxUI() {

        override fun selectNextPossibleValue() {
            val index = listBox.selectedIndex
            if (index < comboBox.model.size - 1) {
                setSelectedIndex(index + 1)
            }
        }

        override fun selectPreviousPossibleValue() {
            val index = listBox.selectedIndex
            if (index > 0) {
                setSelectedIndex(index - 1)
            }
        }

        private fun setSelectedIndex(index: Int) {
            listBox.selectedIndex = index
            listBox.ensureIndexIsVisible(index)
            comboBox.repaint()
        }
    }
}
