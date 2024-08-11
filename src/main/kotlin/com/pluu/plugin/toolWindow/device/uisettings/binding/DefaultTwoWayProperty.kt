///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/binding/DefaultTwoWayProperty.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.binding

import com.android.tools.idea.util.ListenerCollection

/**
 * Standard implementation of a [TwoWayProperty].
 */
internal open class DefaultTwoWayProperty<T>(initialValue: T) : TwoWayProperty<T> {
  private val listeners = ListenerCollection.createWithDirectExecutor<ChangeListener<T>>()
  private var actualValue = initialValue
  private val emptyChangeListener = ChangeListener<T> {}
  override var uiChangeListener = emptyChangeListener
    set(value) {
      if (field === emptyChangeListener || value === emptyChangeListener) field = value else error("uiChangeListener is already specified")
    }

  override fun clearUiChangeListener() {
    uiChangeListener = emptyChangeListener
  }

  override val value: T
    get() = actualValue

  override fun addControllerListener(listener: ChangeListener<T>) {
    listeners.add(listener)
    listener.valueChanged(value)
  }

  override fun setFromUi(newValue: T) {
    if (newValue != actualValue) {
      actualValue = newValue
      uiChangeListener.valueChanged(newValue)
    }
  }

  override fun setFromController(newValue: T) {
    actualValue = newValue
    listeners.forEach { it.valueChanged(newValue) }
  }

  override fun <U> createMappedProperty(toTarget: (T) -> U, fromTarget: (U) -> T): TwoWayProperty<U> {
    val property = DefaultTwoWayProperty(toTarget(value))
    property.uiChangeListener = ChangeListener { setFromUi(fromTarget(it)) }
    listeners.add { property.setFromController(toTarget(it)) }
    return property
  }

  override fun and(other: ReadOnlyProperty<Boolean>): ReadOnlyProperty<Boolean> {
    if (actualValue !is Boolean) error("Boolean property required")
    val result = DefaultTwoWayProperty((actualValue as Boolean) and other.value)
    addControllerListener { result.setFromController((it as Boolean) and other.value) }
    other.addControllerListener { result.setFromController((actualValue as Boolean) and it) }
    return result
  }

  override fun not(): ReadOnlyProperty<Boolean> {
    if (actualValue !is Boolean) error("Boolean property required")
    val result = DefaultTwoWayProperty(!(actualValue as Boolean))
    addControllerListener { result.setFromController(!(it as Boolean)) }
    return result
  }
}
