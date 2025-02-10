///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceComboBox.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceIcons
import com.android.tools.idea.deviceprovisioner.StudioDefaultDeviceIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_CAR
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_PHONE
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_TV
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_WEAR
import javax.swing.JList

private val PHYSICAL_ICONS = StudioDefaultDeviceIcons
private val EMULATOR_ICONS =
    DeviceIcons(
        VIRTUAL_DEVICE_PHONE,
        VIRTUAL_DEVICE_WEAR,
        VIRTUAL_DEVICE_TV,
        VIRTUAL_DEVICE_CAR
    )

internal class DeviceComboBox : ComboBox<Device>() {
    private val deviceComboModel: DeviceComboModel
        get() = model as DeviceComboModel

    init {
        renderer = DeviceComboBoxRenderer()
        model = DeviceComboModel()
    }

    fun addDevice(device: Device): Device =
        deviceComboModel.addDevice(device)

    fun removeDevice(device: Device) {
        deviceComboModel.removeDevice(device)
    }

    fun replaceDevice(device: Device, setSelected: Boolean) {
        deviceComboModel.replaceDevice(device, setSelected)
    }

    fun containsDevice(device: Device): Boolean =
        deviceComboModel.containsDevice(device)

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
                    append("Select connected Devices", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES)
                } else {
                    append("No Connected Devices", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES)
                }
                return
            }
            renderDevice(item)
        }

        private fun renderDevice(device: Device) {
            val icons = if (device.isEmulator) EMULATOR_ICONS else PHYSICAL_ICONS
            icon = icons.iconForDeviceType(device.type)

            append(device.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (device.isOnline) {
                append(" ${device.serialNumber}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
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
}