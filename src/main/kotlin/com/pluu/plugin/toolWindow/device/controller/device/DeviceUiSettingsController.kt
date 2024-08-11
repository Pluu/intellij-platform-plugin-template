///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/device/DeviceUiSettingsController.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.controller.device

import com.android.tools.idea.concurrency.AndroidCoroutineScope
import com.google.wireless.android.sdk.stats.DeviceInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.pluu.plugin.toolWindow.device.uisettings.status.UiSettingsStats
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsController
import com.pluu.plugin.toolWindow.device.uisettings.ui.UiSettingsModel
import kotlinx.coroutines.launch

/**
 * A controller for the UI settings for a physical device,
 * that populates the model and reacts to changes to the model initiated by the UI.
 */
internal class DeviceUiSettingsController(
    private val deviceController: DeviceController,
    deviceInfo: DeviceInfo,
    private val project: Project,
    model: UiSettingsModel,
    parentDisposable: Disposable
) : UiSettingsController(model, UiSettingsStats(deviceInfo)) {
    private val scope = AndroidCoroutineScope(parentDisposable)

    override suspend fun populateModel() {
        populateModel(deviceController.getUiSettings())
    }

    private fun populateModel(response: UiSettingsResponse) {
        model.inDarkMode.setFromController(response.darkMode)
        model.fontScaleInPercent.setFromController(response.fontScale)
        model.screenDensity.setFromController(response.density)
        model.talkBackOn.setFromController(response.talkBackOn)
        model.selectToSpeakOn.setFromController(response.selectToSpeakOn)
        model.gestureNavigation.setFromController(response.gestureNavigation)
//        model.debugLayout.setFromController(response.debugLayout)
//        AppLanguageService.getInstance(project).getAppLanguageInfo(
//            RunningApplicationIdentity(applicationId = response.foregroundApplicationId, processName = null))?.let {
//            addLanguage(it.applicationId, it.localeConfig, response.appLocale)
//        }
        model.differentFromDefault.setFromController(!response.originalValues)
        model.fontScaleSettable.setFromController(response.fontScaleSettable)
        model.screenDensitySettable.setFromController(response.densitySettable)
        model.talkBackInstalled.setFromController(response.tackBackInstalled)
        model.gestureOverlayInstalled.setFromController(response.gestureOverlayInstalled)
    }

    private fun handleCommandResponse(response: UiSettingsChangeResponse) {
        model.differentFromDefault.setFromController(!response.originalValues)
    }

    override fun setDarkMode(on: Boolean) {
        scope.launch {
            handleCommandResponse(deviceController.setDarkMode(on))
        }
    }

    override fun setFontScale(percent: Int) {
        scope.launch {
            handleCommandResponse(deviceController.setFontScale(percent))
        }
    }

    override fun setScreenDensity(density: Int) {
        scope.launch {
            handleCommandResponse(deviceController.setScreenDensity(density))
        }
    }

    override fun setTalkBack(on: Boolean) {
        scope.launch {
            handleCommandResponse(deviceController.setTalkBack(on))
        }
    }

    override fun setSelectToSpeak(on: Boolean) {
        scope.launch {
            handleCommandResponse(deviceController.setSelectToSpeak(on))
        }
    }

    override fun setGestureNavigation(on: Boolean) {
        scope.launch {
            handleCommandResponse(deviceController.setGestureNavigation(on))
        }
    }

    override fun reset() {
        scope.launch {
            populateModel(deviceController.resetUiSettings())
        }
    }
}
