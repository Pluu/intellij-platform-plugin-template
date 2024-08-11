///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/device/AndroidKeyEventActionType.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.controller.device

/**
 * Keyboard event actions. See https://developer.android.com/reference/android/view/KeyEvent.
 */
internal enum class AndroidKeyEventActionType(val value: Int) {
    /** The key has been pressed down. See android.view.KeyEvent.ACTION_DOWN. */
    ACTION_DOWN(0),

    /** The key has been released. See android.view.KeyEvent.ACTION_UP. */
    ACTION_UP(1),

    /** The key has been pressed and released. */
    ACTION_DOWN_AND_UP(8);

    companion object {
        @JvmStatic
        fun fromValue(value: Int): AndroidKeyEventActionType? = entries.find { it.value == value }
    }
}