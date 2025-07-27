///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceUtils.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.tracker

import com.android.adblib.serialNumber
import com.android.sdklib.AndroidVersion
import com.android.sdklib.SdkVersionInfo.HIGHEST_KNOWN_STABLE_API
import com.android.sdklib.deviceprovisioner.DeviceState
import com.android.sdklib.deviceprovisioner.DeviceType
import com.android.sdklib.deviceprovisioner.LocalEmulatorProperties
import com.pluu.plugin.toolWindow.device.Device
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsModel
import java.awt.Dimension
import kotlin.io.path.pathString

/** Convert a [DeviceState] to a [Device] */
internal fun DeviceState.toDevice(): Device? {
    val serialNumber = connectedDevice?.serialNumber ?: return null
    val properties = this.properties

    val release = properties.androidRelease ?: "Unknown"
    val manufacturer = properties.manufacturer ?: "Unknown"
    val model = properties.model ?: "Unknown"
    val androidVersion = properties.androidVersion ?: AndroidVersion(HIGHEST_KNOWN_STABLE_API, 0)

    val screenSize = properties.resolution?.let { Dimension(it.width, it.height) } ?: return null
    val density = properties.density ?: return null

    val uiSettingsModel = UiSettingsModel(
        screenSize,
        density,
        androidVersion.androidApiLevel.majorVersion,
        properties.deviceType ?: DeviceType.HANDHELD
    )

    return when (properties) {
        is LocalEmulatorProperties -> {
            Device.createEmulator(
                serialNumber,
                true,
                release,
                androidVersion,
                properties.displayName,
                properties.avdPath.pathString,
                properties.deviceType,
                uiSettingsModel
            )
        }

        else ->
            Device.createPhysical(
                serialNumber,
                true,
                release,
                androidVersion,
                manufacturer,
                model,
                properties.deviceType,
                uiSettingsModel
            )
    }
}
