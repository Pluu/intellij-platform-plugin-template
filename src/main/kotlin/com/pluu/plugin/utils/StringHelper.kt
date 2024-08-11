package com.pluu.plugin.utils

/** Checks if the string interpreted as a separated list contains the given element. */
fun CharSequence.asSeparatedListContains(
    element: CharSequence,
    separators: CharSequence = ","
): Boolean {
    var offset = 0
    while (offset < length) {
        if (startsWith(element, offset) &&
            (length == offset + element.length ||
                    separators.contains(this[offset + element.length]))) {
            return true;
        }
        @Suppress("ControlFlowWithEmptyBody")
        while (offset < length && !separators.contains(this[offset++])) {}
    }
    return false
}