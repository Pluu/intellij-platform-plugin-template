///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/DeviceEvent.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.tracker

import com.pluu.plugin.toolWindow.device.Device

/** An event that signals a device has been added on changed. */
internal sealed class DeviceEvent {
  data class Added(val device: Device) : DeviceEvent()

  data class StateChanged(val device: Device) : DeviceEvent()
}