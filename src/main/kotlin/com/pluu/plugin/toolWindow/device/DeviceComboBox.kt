///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceComboBox.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceIcons
import com.android.tools.idea.deviceprovisioner.StudioDefaultDeviceIcons
import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.components.BorderLayoutPanel
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_CAR
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_PHONE
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_TV
import icons.StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_WEAR
import java.awt.Component
import javax.swing.JList

private val PHYSICAL_ICONS = StudioDefaultDeviceIcons
private val EMULATOR_ICONS =
    DeviceIcons(VIRTUAL_DEVICE_PHONE, VIRTUAL_DEVICE_WEAR, VIRTUAL_DEVICE_TV, VIRTUAL_DEVICE_CAR)

internal class DeviceComboBox : ComboBox<Device>() {
    private val deviceComboModel: DeviceComboModel
        get() = model as DeviceComboModel

    init {
        renderer = DeviceComboBoxRenderer()
        model = DeviceComboModel()
    }

    override fun updateUI() {
        setUI(DeviceComboBoxUi())
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

            append(device.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (device.isOnline) {
                append(" (${device.serialNumber})", SimpleTextAttributes.REGULAR_ATTRIBUTES)
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