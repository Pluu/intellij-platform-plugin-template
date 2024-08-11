///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/ui/UiComboBoxModel.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.ui

import com.intellij.ui.layout.ComponentPredicate
import com.pluu.plugin.toolWindow.device.uisettings.binding.DefaultTwoWayProperty
import com.pluu.plugin.toolWindow.device.uisettings.binding.TwoWayProperty
import javax.swing.DefaultComboBoxModel

/**
 * A MutableComboBoxModel with a [TwoWayProperty] controlling the selection and a predicate for the presence of multiple elements.
 */
internal class UiComboBoxModel<T>(initialValue: T): DefaultComboBoxModel<T>() {

  val selection: TwoWayProperty<T> = object : DefaultTwoWayProperty<T>(initialValue) {
    override fun setFromUi(newValue: T) {
      super.setFromUi(newValue)
      this@UiComboBoxModel.selectedItem = newValue
    }
  }

  fun sizeIsAtLeast(count: Int): ComponentPredicate {
    return object : ComponentPredicate() {
      override fun invoke(): Boolean = size >= count
      override fun addListener(listener: (Boolean) -> Unit) {}
    }
  }
}