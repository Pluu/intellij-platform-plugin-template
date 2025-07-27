///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/Device.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.android.sdklib.AndroidApiLevel
import com.android.sdklib.AndroidVersion
import com.android.sdklib.deviceprovisioner.DeviceType
import com.android.tools.idea.ui.screenshot.ScreenshotParameters
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsModel
import java.nio.file.Path

/** A representation of a device used by [DeviceComboBox]. */
internal sealed class Device() {
    abstract val deviceId: String
    abstract val name: String
    abstract val serialNumber: String
    abstract val isOnline: Boolean
    abstract val release: String
    abstract val apiLevel: AndroidApiLevel
    abstract val featureLevel: Int
    abstract val type: DeviceType?
    abstract val uiSettingsModel: UiSettingsModel

    val sdk: Int
        get() = apiLevel.majorVersion

    abstract val isEmulator: Boolean

    abstract fun getScreenshotParameters(): ScreenshotParameters

    abstract fun copy(
        isOnline: Boolean = this.isOnline,
        apiLevel: AndroidApiLevel = this.apiLevel,
    ): Device

    data class PhysicalDevice(
        override val serialNumber: String,
        override val isOnline: Boolean,
        override val release: String,
        override val apiLevel: AndroidApiLevel,
        override val featureLevel: Int,
        val manufacturer: String,
        val model: String,
        override val type: DeviceType,
        override val uiSettingsModel: UiSettingsModel
    ) : Device() {
        override val deviceId: String
            get() = serialNumber

        override val name: String
            get() = if (model.startsWith(manufacturer)) model else "$manufacturer $model"

        override val isEmulator
            get() = false

        override fun getScreenshotParameters() = ScreenshotParameters(serialNumber, type, model)

        override fun copy(isOnline: Boolean, apiLevel: AndroidApiLevel) =
            PhysicalDevice(
                serialNumber,
                isOnline,
                release,
                apiLevel,
                featureLevel,
                manufacturer,
                model,
                type,
                uiSettingsModel
            )
    }

    data class EmulatorDevice(
        override val serialNumber: String,
        override val isOnline: Boolean,
        override val release: String,
        override val apiLevel: AndroidApiLevel,
        override val featureLevel: Int,
        val avdName: String,
        val avdPath: String,
        override val type: DeviceType,
        override val uiSettingsModel: UiSettingsModel
    ) : Device() {
        override val isEmulator
            get() = true

        override val deviceId: String
            get() = avdPath

        override val name: String
            get() = avdName

        override fun getScreenshotParameters() =
            ScreenshotParameters(serialNumber, type, Path.of(avdPath))

        override fun copy(isOnline: Boolean, apiLevel: AndroidApiLevel) =
            EmulatorDevice(
                serialNumber,
                isOnline,
                release,
                apiLevel,
                featureLevel,
                avdName,
                avdPath,
                type,
                uiSettingsModel
            )
    }

    companion object {
        fun createPhysical(
            serialNumber: String,
            isOnline: Boolean,
            release: String,
            androidVersion: AndroidVersion,
            manufacturer: String,
            model: String,
            type: DeviceType? = null,
            uiSettingsModel: UiSettingsModel
        ): Device {
            return PhysicalDevice(
                serialNumber,
                isOnline,
                release.normalizeVersion(),
                androidVersion.androidApiLevel,
                androidVersion.featureLevel,
                manufacturer,
                model,
                type ?: DeviceType.HANDHELD,
                uiSettingsModel
            )
        }

        fun createEmulator(
            serialNumber: String,
            isOnline: Boolean,
            release: String,
            androidVersion: AndroidVersion,
            avdName: String,
            avdPath: String,
            type: DeviceType? = null,
            uiSettingsModel: UiSettingsModel
        ): Device {
            return EmulatorDevice(
                serialNumber,
                isOnline,
                release.normalizeVersion(),
                androidVersion.androidApiLevel,
                androidVersion.featureLevel,
                avdName,
                avdPath,
                type ?: DeviceType.HANDHELD,
                uiSettingsModel
            )
        }
    }
}

private val VERSION_TRAILING_ZEROS_REGEX = "(\\.0)+$".toRegex()

private fun String.normalizeVersion(): String {
    return VERSION_TRAILING_ZEROS_REGEX.replace(this, "")
}