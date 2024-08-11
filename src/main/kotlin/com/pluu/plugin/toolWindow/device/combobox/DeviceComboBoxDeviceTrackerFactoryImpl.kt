///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceComboBoxDeviceTrackerFactoryImpl.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.combobox

import com.intellij.openapi.project.Project
import com.pluu.plugin.toolWindow.device.Device

/** Creates a [DeviceComboBoxDeviceTracker] in production code. */
internal class DeviceComboBoxDeviceTrackerFactoryImpl(private val project: Project) :
    DeviceComboBoxDeviceTrackerFactory {
  override fun createDeviceComboBoxDeviceTracker(
    preexistingDevice: Device?
  ): IDeviceComboBoxDeviceTracker {
    return DeviceComboBoxDeviceTracker(project, preexistingDevice)
  }
}