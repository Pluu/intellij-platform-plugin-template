package com.pluu.plugin.toolWindow.designsystem

data class DesignSystemType(
    val name: String,
    val isSelectable: Boolean = true
) {
    val displayName: String = name.uppercase()

    companion object {

        val NONE = DesignSystemType("", false)
    }
}

