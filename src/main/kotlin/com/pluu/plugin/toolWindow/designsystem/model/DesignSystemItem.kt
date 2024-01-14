package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCacheValue

data class DesignSystemItem(
    val type: DesignSystemType,
    override val name: String,
    val aliasNames: List<String>?,
    val file: VirtualFile?,
    val sampleCode: String?
) : ImageCacheValue {
    override val modificationStamp: Long
        get() = file?.modificationStamp ?: -1L

    override val key: Any
        get() = AssetKey(name, type, null)

    fun isValidate(): Boolean {
        return name.isNotEmpty() && file != null && !sampleCode.isNullOrEmpty()
    }
}

/**
 * A light-weight class to represent [DesignSystemItem] instances.
 */
data class AssetKey(
    val name: String,
    val type: DesignSystemType,
    val path: String?
)
