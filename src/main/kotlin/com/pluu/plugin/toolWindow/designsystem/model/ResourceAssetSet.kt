package com.pluu.plugin.toolWindow.designsystem.model

data class DesignAssetSet(
    val name: String,
    val asset: DesignSystemItem
) {
    fun isValidate(): Boolean {
        return name.isNotEmpty() && asset.isValidate()
    }
}