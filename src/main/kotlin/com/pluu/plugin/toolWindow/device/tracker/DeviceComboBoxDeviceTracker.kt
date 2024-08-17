///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceComboBoxDeviceTracker.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.tracker

import com.android.adblib.serialNumber
import com.android.sdklib.deviceprovisioner.DeviceProvisioner
import com.android.sdklib.deviceprovisioner.DeviceState
import com.android.sdklib.deviceprovisioner.mapStateNotNull
import com.pluu.plugin.toolWindow.device.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** An implementation of IDeviceComboBoxDeviceTracker that uses an [AdbSession] */
internal class DeviceComboBoxDeviceTracker(
    private val deviceProvisioner: DeviceProvisioner
) : IDeviceComboBoxDeviceTracker {

    override suspend fun trackDevices(): Flow<DeviceEvent> {
        val onlineDevicesBySerial = mutableMapOf<String, Device>()
        val allDevicesById = mutableMapOf<String, Device>()

        // Initialize state by reading all current devices
        return flow {
            val initialDevices = deviceProvisioner.devices.value
            initialDevices
                .filter { it.state.isOnline() }
                .mapNotNull { it.state.toDevice() }
                .forEach { device ->
                    onlineDevicesBySerial[device.serialNumber] = device
                    allDevicesById[device.deviceId] = device
                    emit(DeviceEvent.Added(device))
                }

            // Track devices changes:
            // We only care about devices that are online.
            // If a previously unknown device comes online, we emit Added
            // If a previously known device comes online, we emit StateChanged
            // If previously online device is missing from the list, we emit a StateChanged.
            deviceProvisioner
                .mapStateNotNull { _, state -> state.asConnectedReady() }
                .collect { states ->
                    val onlineStatesBySerial = states.associateBy { it.connectedDevice.serialNumber }
                    onlineStatesBySerial.values.forEach { state ->
                        val device = state.toDevice() ?: return@forEach
                        if (!onlineDevicesBySerial.containsKey(device.serialNumber)) {
                            if (allDevicesById.containsKey(device.deviceId)) {
                                emit(DeviceEvent.StateChanged(device))
                            } else {
                                emit(DeviceEvent.Added(device))
                            }
                            onlineDevicesBySerial[device.serialNumber] = device
                            allDevicesById[device.deviceId] = device
                        }
                    }

                    // Find devices that were online and are not anymore, then remove them.
                    onlineDevicesBySerial.keys
                        .filter { !onlineStatesBySerial.containsKey(it) }
                        .forEach {
                            val device = onlineDevicesBySerial[it] ?: return@forEach
                            val deviceOffline = device.copy(isOnline = false)
                            emit(DeviceEvent.StateChanged(deviceOffline))
                            onlineDevicesBySerial.remove(it)
                            allDevicesById[device.deviceId] = deviceOffline
                        }
                }
        }
            .flowOn(Dispatchers.IO)
    }
}

private fun DeviceState.asConnectedReady() = takeIf { it.isReady } as? DeviceState.Connected