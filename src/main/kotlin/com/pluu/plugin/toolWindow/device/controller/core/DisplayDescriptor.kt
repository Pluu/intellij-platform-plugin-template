///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/core/DisplayDescriptor.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device.controller.core

import com.android.tools.idea.streaming.core.DisplayType
import java.awt.Dimension

/** XML-serializable descriptor of a device display. */
internal data class DisplayDescriptor(
    var displayId: Int,
    var width: Int,
    var height: Int,
    var orientation: Int = 0,
    var type: DisplayType = DisplayType.UNKNOWN
) : Comparable<DisplayDescriptor> {

    constructor(displayId: Int, size: Dimension, orientation: Int = 0, type: DisplayType = DisplayType.UNKNOWN) :
            this(displayId, size.width, size.height, orientation, type)

    @Suppress("unused") // Used by XML deserializer.
    constructor() : this(0, 0, 0)

    val size
        get() = Dimension(width, height)

    override fun compareTo(other: DisplayDescriptor): Int {
        return displayId - other.displayId
    }
}
