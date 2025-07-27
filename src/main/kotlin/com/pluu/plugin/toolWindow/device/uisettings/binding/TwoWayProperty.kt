///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/uisettings/binding/TwoWayProperty.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.uisettings.binding

/**
 * General change listener.
 */
internal fun interface ChangeListener<T> {
  fun valueChanged(newValue: T)
}

/**
 * A property of type [T] that is intended to be used in UI models.
 * A [ReadOnlyProperty] is read but not changed from the UI, however it can be changed from the controller.
 */
internal interface ReadOnlyProperty<T> {
  /**
   * The actual value of this property.
   */
  val value: T

  /**
   * The UI can be notified of changes from the controller by adding a listener.
   */
  fun addControllerListener(listener: ChangeListener<T>)

  /**
   * The controller should call this function to specify a new value.
   */
  fun setFromController(newValue: T)

  /**
   * Creates a boolean property that yields true only if both the
   * current boolean property and [other] is true.
   */
  fun and(other: ReadOnlyProperty<Boolean>): ReadOnlyProperty<Boolean>

  /**
   * Creates a boolean property that yields the opposite value of the
   * current boolean property.
   */
  fun not(): ReadOnlyProperty<Boolean>
}

/**
 * A property of type [T] that can be changed either from the UI or from the controller.
 * Note: the controller listeners are NOT fired when the property is changed by [setFromUi],
 * and the [uiChangeListener] is NOT fired when the property is changed by [setFromController].
 */
internal interface TwoWayProperty<T> : ReadOnlyProperty<T> {
  /**
   * A controller should supply a [uiChangeListener] if changes from the UI are expected.
   */
  var uiChangeListener: ChangeListener<T>

  /**
   * Remove the current uiChangeListener
   */
  fun clearUiChangeListener()

  /**
   * The UI should call this function to specify a new value.
   */
  fun setFromUi(newValue: T)

  /**
   * Create a property of type [U] that maps a value from this property via
   * the functions: [toTarget] and the inverse function [fromTarget].
   */
  fun <U> createMappedProperty(toTarget: (T) -> U, fromTarget: (U) -> T): TwoWayProperty<U>
}
