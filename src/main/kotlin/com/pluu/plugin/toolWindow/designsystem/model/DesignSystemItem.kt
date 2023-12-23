package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCacheValue

data class DesignSystemItem(
    val type: DesignSystemType,
    override val name: String,
    val file: VirtualFile
) : ImageCacheValue {
    override val modificationStamp: Long
        get() = file.modificationStamp

    override val key: Any
        get() = AssetKey(name, type, null)
}

/**
 * A light-weight class to represent [DesignSystemItem] instances.
 */
data class AssetKey(
    val name: String,
    val type: DesignSystemType,
    val path: String?
)
