///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceComboBox.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceIcons
import com.android.sdklib.deviceprovisioner.DeviceProvisioner
import com.android.tools.idea.deviceprovisioner.StudioDefaultDeviceIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.pluu.plugin.PluuBundle
import com.pluu.plugin.toolWindow.device.tracker.DeviceComboBoxDeviceTracker
import com.pluu.plugin.toolWindow.device.tracker.DeviceEvent.Added
import com.pluu.plugin.toolWindow.device.tracker.DeviceEvent.StateChanged
import com.pluu.plugin.toolWindow.device.tracker.IDeviceComboBoxDeviceTracker
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_CAR
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_HEADSET
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_PHONE
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_TV
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_WEAR
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.awt.event.ActionListener
import javax.swing.JList

private val PHYSICAL_ICONS = StudioDefaultDeviceIcons
private val EMULATOR_ICONS =
    DeviceIcons(
        VIRTUAL_DEVICE_PHONE,
        VIRTUAL_DEVICE_WEAR,
        VIRTUAL_DEVICE_TV,
        VIRTUAL_DEVICE_CAR,
        VIRTUAL_DEVICE_HEADSET
    )

internal class DeviceComboBox(
    deviceProvisioner: DeviceProvisioner
) : ComboBox<Device>() {

    private val deviceTracker: IDeviceComboBoxDeviceTracker =
        DeviceComboBoxDeviceTracker(deviceProvisioner)

    private val deviceComboModel: DeviceComboModel
        get() = model as DeviceComboModel

    init {
        renderer = DeviceComboBoxRenderer()
        model = DeviceComboModel()
    }

    fun addDevice(device: Device): Device =
        deviceComboModel.addDevice(device)

    fun replaceDevice(device: Device, setSelected: Boolean) {
        deviceComboModel.replaceDevice(device, setSelected)
    }

    fun containsDevice(device: Device): Boolean =
        deviceComboModel.containsDevice(device)

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
        if (containsDevice(device)) {
            deviceStateChanged(device)
        } else {
            val item = addDevice(device)
            when {
                selectedItem != null -> return
                else -> selectItem(item)
            }
        }
    }

    private fun selectItem(item: Device?) {
        selectedItem = item
    }

    private fun deviceStateChanged(device: Device) {
        when (containsDevice(device)) {
            true ->
                replaceDevice(
                    device,
                    device.deviceId == item.deviceId,
                )

            false -> deviceAdded(device) // Device was removed manually so we re-add it
        }
    }

    class DeviceComboBoxRenderer : ColoredListCellRenderer<Device>() {
        override fun customizeCellRenderer(
            list: JList<out Device>,
            item: Device?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            if (item == null) {
                if (list.model.size > 0) {
                    append("Select connected Devices", GRAYED_BOLD_ATTRIBUTES)
                } else {
                    append("No Connected Devices", GRAYED_BOLD_ATTRIBUTES)
                }
                return
            }
            renderDevice(item)
        }

        private fun renderDevice(device: Device) {
            val icons = if (device.isEmulator) EMULATOR_ICONS else PHYSICAL_ICONS
            icon = icons.iconForDeviceType(device.type)

            append(device.name, REGULAR_ATTRIBUTES)
            if (device.isOnline) {
                append(" ${device.serialNumber}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            append(
                PluuBundle.message("logcat.device.combo.version", device.release, device.apiLevel),
                GRAY_ATTRIBUTES,
            )
            if (!device.isOnline) {
                append(PluuBundle.message("logcat.device.combo.offline"), GRAYED_BOLD_ATTRIBUTES)
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

        fun containsDevice(device: Device): Boolean =
            items.find { it.deviceId == device.deviceId } != null
    }
}