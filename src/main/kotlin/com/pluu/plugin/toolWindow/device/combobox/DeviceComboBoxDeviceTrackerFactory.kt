///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceComboBoxDeviceTrackerFactory.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.combobox

import com.intellij.openapi.project.Project
import com.pluu.plugin.toolWindow.device.Device

/** A [Project] service that creates an [IDeviceComboBoxDeviceTracker] */
internal fun interface DeviceComboBoxDeviceTrackerFactory {
  fun createDeviceComboBoxDeviceTracker(preexistingDevice: Device?): IDeviceComboBoxDeviceTracker

  companion object {
    fun getInstance(project: Project): DeviceComboBoxDeviceTrackerFactory =
      project.getService(DeviceComboBoxDeviceTrackerFactory::class.java)
  }
}