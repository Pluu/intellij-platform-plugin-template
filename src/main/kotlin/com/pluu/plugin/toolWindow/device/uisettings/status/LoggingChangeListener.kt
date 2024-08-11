///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/stats/LoggingChangeListener.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.status

import com.pluu.plugin.toolWindow.device.uisettings.binding.ChangeListener

/**
 * [ChangeListener] that also logs the change.
 */
internal class LoggingChangeListener<T>(
  private val listener: ChangeListener<T>,
  private val logger: () -> Unit
): ChangeListener<T> {
  override fun valueChanged(newValue: T) {
    listener.valueChanged(newValue)
    logger()
  }
}
