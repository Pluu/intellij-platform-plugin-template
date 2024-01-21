package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCacheValue

/** [DataKey] to pass an array of [DesignSystemItem]s. */
val RESOURCE_DESIGN_ASSETS_KEY: DataKey<List<DesignSystemItem>> = DataKey.create("DesignSystem Assets Key")

data class DesignSystemItem(
    val type: DesignSystemType,
    override val name: String,
    val aliasNames: List<String>?,
    val file: VirtualFile?,
    val applicableFileType: ApplicableFileType,
    val sampleCode: String?
) : ImageCacheValue {
    override val modificationStamp: Long
        get() = file?.modificationStamp ?: -1L

    override val key: Any
        get() = AssetKey(name, type, null)

    fun isValidate(): Boolean {
        return type.isSelectable() &&
                name.isNotEmpty() &&
                file != null &&
                applicableFileType.isSelectable() &&
                !sampleCode.isNullOrEmpty()
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

enum class ApplicableFileType {
    NONE, XML, KOTLIN;

    fun isSelectable(): Boolean = this != NONE

    companion object {
        fun selectableTypes(): Array<ApplicableFileType> = ApplicableFileType.values()
            .filter { it.isSelectable() }
            .toTypedArray()

        fun of(name: String?): ApplicableFileType {
            return ApplicableFileType.values()
                .firstOrNull { it.name == name } ?: defaultType
        }

        val defaultType: ApplicableFileType = XML
    }
}
