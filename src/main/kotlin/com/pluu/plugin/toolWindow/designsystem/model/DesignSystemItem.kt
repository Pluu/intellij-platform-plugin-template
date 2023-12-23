package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType

data class DesignSystemItem(
    val type: DesignSystemType,
    val name: String,
    val file: VirtualFile
) {
    val modificationStamp: Long
        get() = file.modificationStamp

    val key: AssetKey
        get() = AssetKey(name, type, null)
}

data class AssetKey(
    val name: String,
    val type: DesignSystemType,
    val path: String?
)
