///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/ui/UiSettingsPanel.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.ui

import com.android.sdklib.deviceprovisioner.DeviceType
import com.android.tools.adtui.common.AdtUiUtils
import com.android.tools.adtui.common.secondaryPanelBackground
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.IntelliJSpacingConfiguration
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.actionListener
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.pluu.plugin.toolWindow.device.uisettings.binding.ReadOnlyProperty
import com.pluu.plugin.toolWindow.device.uisettings.binding.TwoWayProperty
import java.awt.Component
import java.awt.Container
import java.awt.KeyboardFocusManager
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JSlider
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.ListCellRenderer
import javax.swing.SwingUtilities
import javax.swing.plaf.basic.BasicComboBoxEditor

private const val TITLE = "Device Settings Shortcuts"
private const val LANGUAGE_WIDTH = 200
internal const val DARK_THEME_TITLE = "Dark Theme:"
internal const val GESTURE_NAVIGATION_TITLE = "Navigation Mode:"
internal const val APP_LANGUAGE_TITLE = "App Language:"
internal const val TALKBACK_TITLE = "TalkBack:"
internal const val SELECT_TO_SPEAK_TITLE = "Select to Speak:"
internal const val FONT_SCALE_TITLE = "Font Size:"
internal const val DENSITY_TITLE = "Display Size:"
internal const val DEBUG_LAYOUT_TITLE = "Debug Layout:"
internal const val RESET_TITLE = "Reset"
internal const val PERMISSION_HINT_LINE1 =
    "More options may be available if \"Disable permission monitoring\" is turned on in"
internal const val PERMISSION_HINT_LINE2 = "\"Developer Options\" and the device is restarted."

/**
 * Custom horizontal spacing between labels and controls.
 */
private val SPACING = object : IntelliJSpacingConfiguration() {
    override val horizontalSmallGap = 50
}

/**
 * Displays a picker with setting shortcuts.
 *
 * @param model the UI settings model
 * @param deviceType some controls are only available for certain device types
 */
internal class UiSettingsPanel(
    private val model: UiSettingsModel,
    deviceType: DeviceType
) : BorderLayoutPanel() {
    init {
        add(panel {
            border = JBUI.Borders.empty(6)

            row(title(TITLE)) {
                link(RESET_TITLE) { model.resetAction() }
                    .accessibleName(RESET_TITLE)
                    .apply { component.name = RESET_TITLE }
                    .visibleIf(model.differentFromDefault)
                    .align(AlignX.RIGHT)
            }
            separator()

            if (deviceType != DeviceType.WEAR) {
                row(JBLabel(DARK_THEME_TITLE)) {
                    checkBox("")
                        .accessibleName(DARK_THEME_TITLE)
                        .bind(model.inDarkMode)
                        .apply { component.name = DARK_THEME_TITLE }
                }
            }

            if (deviceType == DeviceType.HANDHELD) {
                row(JBLabel(GESTURE_NAVIGATION_TITLE)) {
                    comboBox(model.navigationModel)
                        .accessibleName(GESTURE_NAVIGATION_TITLE)
                        .bindItem(model.navigationModel.selection)
                        .apply {
                            component.name = GESTURE_NAVIGATION_TITLE
                            component.renderer = ListCellRenderer { _, value, _, _, _ ->
                                JBLabel(if (value == true) "Gestures" else "Buttons")
                            }
                        }
                }.visibleIf(model.permissionMonitoringDisabled.and(model.gestureOverlayInstalled))
            }

            row(JBLabel(FONT_SCALE_TITLE)) {
                slider(0, model.fontScaleMaxIndex.value, 1, 1)
                    .accessibleName(FONT_SCALE_TITLE)
                    .noLabels()
                    .align(Align.FILL)
                    .bindSliderPosition(model.fontScaleIndex)
                    .bindSliderMaximum(model.fontScaleMaxIndex)
                    .apply { component.name = FONT_SCALE_TITLE }
            }.visibleIf(model.permissionMonitoringDisabled)

            if (deviceType == DeviceType.HANDHELD) {
                row(JBLabel(DENSITY_TITLE)) {
                    slider(0, model.screenDensityIndex.value, 1, 1)
                        .accessibleName(DENSITY_TITLE)
                        .noLabels()
                        .align(Align.FILL)
                        .bindSliderPosition(model.screenDensityIndex)
                        .bindSliderMaximum(model.screenDensityMaxIndex)
                        .apply { component.name = DENSITY_TITLE }
                }.visibleIf(model.permissionMonitoringDisabled)
            }

            row(JBLabel(DEBUG_LAYOUT_TITLE)) {
                checkBox("")
                    .accessibleName(DEBUG_LAYOUT_TITLE)
                    .bind(model.debugLayout)
                    .apply { component.name = DEBUG_LAYOUT_TITLE }
            }

            row {
                cell(BorderLayoutPanel().apply {
                    addToTop(JBLabel(PERMISSION_HINT_LINE1, UIUtil.ComponentStyle.MINI))
                    addToBottom(JBLabel(PERMISSION_HINT_LINE2, UIUtil.ComponentStyle.MINI))
                })
            }.visibleIf(model.permissionMonitoringDisabled.not())
        })
        updateBackground()

        isFocusCycleRoot = true
        isFocusTraversalPolicyProvider = true
        focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
            override fun getFirstComponent(container: Container): Component? {
                val first = super.getFirstComponent(container) ?: return null
                val from = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                val fromOutside = from == null || !SwingUtilities.isDescendingFrom(from, container)
                return if (first.name == RESET_TITLE && fromOutside) super.getComponentAfter(
                    container,
                    first
                ) else first
            }
        }
    }

    /**
     * Create a label for the title of the panel.
     */
    private fun title(title: String): JBLabel =
        JBLabel(title).apply { foreground = UIUtil.getInactiveTextColor() }

    /**
     * Bind a [Boolean] property to an [AbstractButton] cell.
     */
    private fun <T : AbstractButton> Cell<T>.bind(predicate: TwoWayProperty<Boolean>): Cell<T> {
        predicate.addControllerListener { selected -> component.isSelected = selected }
        component.isSelected = predicate.value
        return actionListener { _, c -> predicate.setFromUi(c.isSelected) }
    }

    private fun <T> Cell<ComboBox<T>>.bindItem(property: TwoWayProperty<T>): Cell<ComboBox<T>> {
        property.addControllerListener { selected -> component.selectedItem = selected }
        component.selectedItem = property.value
        component.addActionListener { property.setFromUi(component.selectedItem as T) }
        return this
    }

    private fun Cell<JSlider>.noLabels(): Cell<JSlider> {
        component.paintLabels = false
        return this
    }

    private fun Cell<JSlider>.bindSliderPosition(property: TwoWayProperty<Int>): Cell<JSlider> {
        property.addControllerListener { component.value = it }
        component.value = property.value
        component.addChangeListener { if (!component.valueIsAdjusting) property.setFromUi(component.value) }
        return this
    }

    private fun Cell<JSlider>.bindSliderMaximum(property: ReadOnlyProperty<Int>): Cell<JSlider> {
        property.addControllerListener { component.maximum = it }
        component.maximum = property.value
        return this
    }

    private fun Row.visibleIf(predicate: ReadOnlyProperty<Boolean>): Row {
        visible(predicate.value)
        predicate.addControllerListener { visible(it) }
        return this
    }

    private fun <T : JComponent> Cell<T>.visibleIf(predicate: ReadOnlyProperty<Boolean>): Cell<T> {
        visible(predicate.value)
        predicate.addControllerListener { visible(it) }
        return this
    }

    /**
     * Use the lighter background color rather than the Swing default.
     */
    private fun updateBackground() {
        AdtUiUtils.allComponents(this).forEach {
            if (it.background is BasicComboBoxEditor.UIResource) {
                it.background = secondaryPanelBackground
            }
        }
    }
}