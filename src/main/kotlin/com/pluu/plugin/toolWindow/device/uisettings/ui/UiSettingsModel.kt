///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/ui/UiSettingsModel.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.ui

import com.android.sdklib.deviceprovisioner.DeviceType
import com.android.tools.idea.streaming.uisettings.ui.GoogleDensityRange
import com.pluu.plugin.toolWindow.device.uisettings.binding.DefaultTwoWayProperty
import com.pluu.plugin.toolWindow.device.uisettings.binding.ReadOnlyProperty
import com.pluu.plugin.toolWindow.device.uisettings.binding.TwoWayProperty
import java.awt.Dimension
import kotlin.math.abs

/**
 * Give 7 choices for font scales. A [percent] of 100 is the normal font scale.
 */
internal enum class FontScale(val percent: Int) {
    SMALL(85),
    NORMAL(100),
    LARGE_115(115),
    LARGE_130(130),
    LARGE_150(150),  // Added in API 34
    LARGE_180(180),  // Added in API 34
    LARGE_200(200);  // Added in API 34

    companion object {
        val scaleMap = FontScale.entries.map { it.percent }
    }
}

/**
 * Give 6 choices for wear devices.
 */
internal enum class WearFontScale(val percent: Int) {
    SMALL(94),
    NORMAL(100),
    MEDIUM(106),
    LARGE(112),
    LARGER(118),
    LARGEST(124);

    companion object {
        val scaleMap = WearFontScale.entries.map { it.percent }
    }
}

/**
 * A model for the [UiSettingsPanel] with bindable properties for getting and setting various Android settings.
 */
internal class UiSettingsModel(screenSize: Dimension, physicalDensity: Int, api: Int, val deviceType: DeviceType) {
    private val densities = GoogleDensityRange.computeDensityRange(screenSize, physicalDensity)

    val inDarkMode: TwoWayProperty<Boolean> = DefaultTwoWayProperty(false)
    val gestureOverlayInstalled: ReadOnlyProperty<Boolean> = DefaultTwoWayProperty(false)
    val navigationModel = UiComboBoxModel(false).apply { addAll(listOf(true, false)) }
    val gestureNavigation: TwoWayProperty<Boolean> = navigationModel.selection
    val talkBackInstalled: ReadOnlyProperty<Boolean> = DefaultTwoWayProperty(false)
    val talkBackOn: TwoWayProperty<Boolean> = DefaultTwoWayProperty(false)
    val selectToSpeakOn: TwoWayProperty<Boolean> = DefaultTwoWayProperty(false)
    val fontScaleInPercent: TwoWayProperty<Int> = DefaultTwoWayProperty(100)
    val fontScaleSettable: ReadOnlyProperty<Boolean> = DefaultTwoWayProperty(true)
    val fontScaleIndex: TwoWayProperty<Int> = fontScaleInPercent.createMappedProperty(::toFontScaleIndex, ::toFontScaleInPercent)
    val fontScaleMaxIndex: ReadOnlyProperty<Int> = DefaultTwoWayProperty(numberOfFontScales(api) - 1)
    val screenDensity: TwoWayProperty<Int> = DefaultTwoWayProperty(physicalDensity)
    val screenDensitySettable: ReadOnlyProperty<Boolean> = DefaultTwoWayProperty(true)
    val screenDensityIndex: TwoWayProperty<Int> = screenDensity.createMappedProperty(::toDensityIndex, ::toDensityFromIndex)
    val screenDensityMaxIndex: ReadOnlyProperty<Int> = DefaultTwoWayProperty(densities.size - 1)
    val debugLayout: TwoWayProperty<Boolean> = DefaultTwoWayProperty(false)
    val differentFromDefault: ReadOnlyProperty<Boolean> = DefaultTwoWayProperty(false)
    var resetAction: () -> Unit = {}

    /***
     * If font scale or density is not settable, we are likely connected to an OEM device that has
     * "Permission Monitoring" turned on. In order to change system & secure settings the user will need to disable
     * this in the developer options.
     */
    val permissionMonitoringDisabled: ReadOnlyProperty<Boolean> = fontScaleSettable.and(screenDensitySettable)

    /**
     * The font scale settings for wear has 6 values, API 33 has 4 values, and for API 34+ there are 7 possible values.
     * See [FontScale]
     */
    private fun numberOfFontScales(api: Int): Int = when {
        deviceType == DeviceType.WEAR -> WearFontScale.entries.size
        api == 33 -> 4
        else -> FontScale.entries.size
    }

    /**
     * The scaleMap for the current device type.
     */
    private val scaleMap: List<Int>
        get() = if (deviceType == DeviceType.WEAR) WearFontScale.scaleMap else FontScale.scaleMap

    private fun toFontScaleInPercent(fontIndex: Int): Int =
        scaleMap[fontIndex.coerceIn(0, fontScaleMaxIndex.value)]

    private fun toFontScaleIndex(percent: Int): Int =
        scaleMap.withIndex().minBy { (_,value) -> abs(value - percent) }.index

    private fun toDensityFromIndex(densityIndex: Int): Int =
        densities[densityIndex.coerceIn(0, screenDensityMaxIndex.value)]

    private fun toDensityIndex(density: Int): Int =
        densities.indexOf(densities.minBy { abs(it - density) })

    fun clearUiChangeListener() {
        inDarkMode.clearUiChangeListener()
        gestureNavigation.clearUiChangeListener()
        talkBackOn.clearUiChangeListener()
        selectToSpeakOn.clearUiChangeListener()
        fontScaleInPercent.clearUiChangeListener()
        fontScaleIndex.clearUiChangeListener()
        screenDensity.clearUiChangeListener()
        screenDensityIndex.clearUiChangeListener()
        debugLayout.clearUiChangeListener()
    }
}
