package com.pluu.plugin.toolWindow.designsystem.model

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/model/DesignAsset.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.rendering.ImageCacheValue

/** [DataKey] to pass an array of [DesignSystemItem]s. */
val RESOURCE_DESIGN_ASSETS_KEY: DataKey<Array<DesignSystemItem>> = DataKey.create("DesignSystem Assets Key")

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
        return name.isNotEmpty() &&
                file != null &&
                applicableFileType.isSelectable() &&
                !sampleCode.isNullOrEmpty()
    }

    val fileNameWithExtension: String
        get() = buildString {
            append(name)
            file?.extension?.let {
                append(".${it}")
            }
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
    None, Compose, Xml, Kotlin;

    fun isSelectable(): Boolean = this != None

    companion object {
        fun selectableTypes(): Array<ApplicableFileType> = ApplicableFileType.entries
            .filter { it.isSelectable() }
            .toTypedArray()

        fun of(name: String?): ApplicableFileType {
            return ApplicableFileType.entries
                .firstOrNull { it.name == name } ?: defaultType
        }

        val defaultType: ApplicableFileType = Xml
    }
}
