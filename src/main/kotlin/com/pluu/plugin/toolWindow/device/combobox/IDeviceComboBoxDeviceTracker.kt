///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:logcat/src/com/android/tools/idea/logcat/devices/IDeviceComboBoxDeviceTracker.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.combobox

import kotlinx.coroutines.flow.Flow

/**
 * Tracks devices for [DeviceComboBox].
 *
 * Notifies when a previously unseen device comes online and when a tracked device changes state.
 * Only offline/online states are tracked.
 *
 * Devices are not removed when they go offline.
 */
internal interface IDeviceComboBoxDeviceTracker {
  suspend fun trackDevices(): Flow<DeviceEvent>
}