///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/ui/UiSettingsController.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.ui

import com.pluu.plugin.toolWindow.device.uisettings.binding.ChangeListener

/**
 * A controller for the [UiSettingsPanel] that populates the model and reacts to changes to the model initiated by the UI.
 */
internal abstract class UiSettingsController(
    /**
     * The model that this controller is interacting with.
     */
    protected val model: UiSettingsModel
) {

    init {
        model.inDarkMode.uiChangeListener = ChangeListener(::setDarkMode)
        model.fontScaleInPercent.uiChangeListener = ChangeListener(::setFontScale)
        model.screenDensity.uiChangeListener = ChangeListener(::setScreenDensity)
        model.talkBackOn.uiChangeListener = ChangeListener(::setTalkBack)
        model.selectToSpeakOn.uiChangeListener = ChangeListener(::setSelectToSpeak)
        model.gestureNavigation.uiChangeListener = ChangeListener(::setGestureNavigation)
        model.debugLayout.uiChangeListener = ChangeListener(::setDebugLayout)
        model.dontKeepActivities.uiChangeListener = ChangeListener(::setDontKeepActivities)
        model.resetAction = { reset(); }
    }

    /**
     * Populate all settings in the model.
     */
    abstract suspend fun populateModel()

    /**
     * Changes the dark mode on the device/emulator.
     */
    protected abstract fun setDarkMode(on: Boolean)

    /**
     * Changes the font scale on the device/emulator.
     */
    protected abstract fun setFontScale(percent: Int)

    /**
     * Changes the screen density on the device/emulator.
     */
    protected abstract fun setScreenDensity(density: Int)

    /**
     * Turns TackBack on or off.
     */
    protected abstract fun setTalkBack(on: Boolean)

    /**
     * Turns Select to Speak on or off.
     */
    protected abstract fun setSelectToSpeak(on: Boolean)

    /**
     * Changes the navigation mode on the device to use gestures instead of buttons.
     */
    protected abstract fun setGestureNavigation(on: Boolean)

    /**
     * Turns debug layout boxes on or off.
     */
    protected abstract fun setDebugLayout(on: Boolean)

    protected abstract fun setDontKeepActivities(on: Boolean)

    /**
     * Reset UI settings to factory defaults.
     */
    protected abstract fun reset()
}
