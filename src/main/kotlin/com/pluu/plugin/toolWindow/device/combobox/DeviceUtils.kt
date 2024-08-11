///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceUtils.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.combobox

import com.android.adblib.serialNumber
import com.android.sdklib.SdkVersionInfo
import com.android.sdklib.deviceprovisioner.DeviceState
import com.android.sdklib.deviceprovisioner.DeviceType
import com.android.sdklib.deviceprovisioner.LocalEmulatorProperties
import com.pluu.plugin.toolWindow.device.Device
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsModel
import java.awt.Dimension

/** Convert a [DeviceState] to a [Device] */
internal fun DeviceState.toDevice(): Device? {
    val serialNumber = connectedDevice?.serialNumber ?: return null
    val properties = this.properties

    val release = properties.androidRelease ?: "Unknown"
    val sdk = properties.androidVersion?.apiLevel ?: SdkVersionInfo.HIGHEST_KNOWN_STABLE_API
    val featureLevel = properties.androidVersion?.featureLevel ?: sdk
    val manufacturer = properties.manufacturer ?: "Unknown"
    val model = properties.model ?: "Unknown"

    val screenSize = properties.resolution?.let { Dimension(it.width, it.height) } ?: return null
    val density = properties.density ?: return null

    val uiSettingsModel = UiSettingsModel(
        screenSize, density, sdk, properties.deviceType ?: DeviceType.HANDHELD
    )

    return when (properties) {
        is LocalEmulatorProperties ->
            Device.createEmulator(
                serialNumber,
                true,
                release,
                sdk,
                properties.avdName,
                featureLevel,
                properties.deviceType,
                uiSettingsModel,
                properties.deviceInfoProto
            )

        else ->
            Device.createPhysical(
                serialNumber,
                true,
                release,
                sdk,
                manufacturer,
                model,
                featureLevel,
                properties.deviceType,
                uiSettingsModel,
                properties.deviceInfoProto
            )
    }
}
