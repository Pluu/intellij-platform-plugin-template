///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/Device.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.android.sdklib.deviceprovisioner.DeviceState
import com.android.sdklib.deviceprovisioner.DeviceType
import com.google.wireless.android.sdk.stats.DeviceInfo
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsModel

/** A representation of a device used by [DeviceComboBox]. */
@Suppress("DataClassPrivateConstructor") // Exposed via copy which we use in tests
internal data class Device
private constructor(
    val deviceId: String,
    val name: String,
    val serialNumber: String,
    val isOnline: Boolean,
    val release: String,
    val sdk: Int,
    val featureLevel: Int,
    val model: String,
    val type: DeviceType?,
    val uiSettingsModel: UiSettingsModel,
    val deviceInfo: DeviceInfo,
    private val deviceStatus: DeviceState,
) {

    val isEmulator: Boolean = serialNumber.startsWith("emulator-")

    val isDeviceOnline: Boolean
        get() = deviceStatus.isOnline()

    companion object {
        fun createPhysical(
            serialNumber: String,
            isOnline: Boolean,
            release: String,
            sdk: Int,
            manufacturer: String,
            model: String,
            featureLevel: Int = sdk,
            type: DeviceType? = null,
            uiSettingsModel: UiSettingsModel,
            deviceInfo: DeviceInfo,
            deviceStatus: DeviceState
        ): Device {
            val deviceName = if (model.startsWith(manufacturer)) model else "$manufacturer $model"
            return Device(
                deviceId = serialNumber,
                name = deviceName,
                serialNumber,
                isOnline,
                release.normalizeVersion(),
                sdk,
                featureLevel,
                model,
                type,
                uiSettingsModel,
                deviceInfo,
                deviceStatus
            )
        }

        fun createEmulator(
            serialNumber: String,
            isOnline: Boolean,
            release: String,
            sdk: Int,
            avdName: String,
            featureLevel: Int = sdk,
            type: DeviceType? = null,
            uiSettingsModel: UiSettingsModel,
            deviceInfo: DeviceInfo,
            deviceStatus: DeviceState
        ): Device {
            return Device(
                deviceId = avdName,
                name = avdName.replace('_', ' '),
                serialNumber,
                isOnline,
                release.normalizeVersion(),
                sdk,
                featureLevel,
                model = "",
                type,
                uiSettingsModel,
                deviceInfo,
                deviceStatus
            )
        }
    }
}

private val VERSION_TRAILING_ZEROS_REGEX = "(\\.0)+$".toRegex()

private fun String.normalizeVersion(): String {
    return VERSION_TRAILING_ZEROS_REGEX.replace(this, "")
}