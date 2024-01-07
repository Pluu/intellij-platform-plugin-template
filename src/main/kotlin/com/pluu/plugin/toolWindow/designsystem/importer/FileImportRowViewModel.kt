package com.pluu.plugin.toolWindow.designsystem.importer

import com.intellij.openapi.util.text.StringUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.qualifiers.QualifierConfigurationViewModel

class FileImportRowViewModel(
    val asset: DesignSystemItem,
    val designSystemType: DesignSystemType,
    val qualifierViewModel: QualifierConfigurationViewModel = QualifierConfigurationViewModel(),
    val removeCallback: (DesignSystemItem) -> Unit
) {
    // TODO get value from actual file
    var updateCallback: (() -> Unit)? = null
    var fileDimension: String = ""
    var fileName: String = asset.file?.name.orEmpty()
    var qualifiers: String = ""
    var fileSize: String = StringUtil.formatFileSize(asset.file?.length ?: 0)

    fun removeFile() {
        removeCallback(asset)
    }
}