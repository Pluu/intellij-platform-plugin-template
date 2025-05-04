package com.pluu.plugin.toolWindow.designsystem.model

data class DesignSystemType(
    val name: String,
    val icon: IconType,
) {
    companion object {
        val NONE = DesignSystemType("", IconType.Etc)

        fun default(name: String) = DesignSystemType(name, IconType.Etc)
    }
}

enum class IconType {
    Text,
    Button,
    Toast,
    Control,
    Etc
}

