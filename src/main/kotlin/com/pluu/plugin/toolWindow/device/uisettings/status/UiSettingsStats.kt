///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/stats/UiSettingsStats.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.status

import com.android.tools.analytics.UsageTracker
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.google.wireless.android.sdk.stats.DeviceInfo
import com.google.wireless.android.sdk.stats.UiDeviceSettingsEvent
import com.google.wireless.android.sdk.stats.UiDeviceSettingsEvent.OperationKind

/**
 * Analytics logger for various events from the UI settings panel.
 */
class UiSettingsStats(private val deviceInfo: DeviceInfo?) {

    fun setDarkMode() = logUiSettingsChange(OperationKind.DARK_THEME)

    fun setGestureNavigation() = logUiSettingsChange(OperationKind.GESTURE_NAVIGATION)

    fun setAppLanguage() = logUiSettingsChange(OperationKind.APP_LANGUAGE)

    fun setTalkBack() = logUiSettingsChange(OperationKind.TALKBACK)

    fun setSelectToSpeak() = logUiSettingsChange(OperationKind.SELECT_TO_SPEAK)

    fun setFontScale() = logUiSettingsChange(OperationKind.FONT_SIZE)

    fun setScreenDensity() = logUiSettingsChange(OperationKind.SCREEN_DENSITY)

    fun setDebugLayout() {
        // TODO:
//        logUiSettingsChange(OperationKind.DEBUG_LAYOUT)
    }

    fun reset() = logUiSettingsChange(OperationKind.RESET)

    private fun logUiSettingsChange(operation: OperationKind) {
        val studioEvent = AndroidStudioEvent.newBuilder()
            .setKind(AndroidStudioEvent.EventKind.UI_DEVICE_SETTINGS_EVENT)
            .setUiDeviceSettingsEvent(UiDeviceSettingsEvent.newBuilder().setOperation(operation))
        if (deviceInfo != null) {
            studioEvent.setDeviceInfo(deviceInfo)
        }
        UsageTracker.log(studioEvent)
    }
}
