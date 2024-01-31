package com.pluu.plugin.toolWindow.designsystem

import com.pluu.plugin.settings.ConfigSettings

data class DesignSystemType(
    val name: String,
    val isSelectable: Boolean = true
) {
    val displayName: String = name.uppercase()

    companion object {

        val NONE = DesignSystemType("", false)

        fun instanceFromConfigure(typeName: String): DesignSystemType {
            return ConfigSettings.getInstance().getTypes()
                .first { it.name.equals(typeName, ignoreCase = true) }
        }
    }
}

