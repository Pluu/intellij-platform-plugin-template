package com.pluu.plugin.toolWindow.designsystem.importer

import com.intellij.openapi.util.text.StringUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem

class FileImportRowViewModel(
    val asset: DesignSystemItem,
    val designSystemType: DesignSystemType,
    val removeCallback: (DesignSystemItem) -> Unit
) {
    // TODO get value from actual file
    var updateCallback: (() -> Unit)? = null
    var fileName: String = asset.file?.name.orEmpty()
    var fileSize: String = StringUtil.formatFileSize(asset.file?.length ?: 0)

    val designSystemTypes: Array<DesignSystemType>
        get() = DesignSystemType.values()

    fun removeFile() {
        removeCallback(asset)
    }
}