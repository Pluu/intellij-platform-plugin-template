package com.pluu.plugin.toolWindow.designsystem.importer

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import javax.swing.JTextArea

class FileImportRowViewModel(
    asset: DesignSystemItem,
    val updateDesignSystemTypeCallback: (DesignSystemType) -> Unit,
    val updateSampleCodeCallback: (String) -> Unit,
    val removeCallback: () -> Unit
) {
    // TODO get value from actual file
    var updateCallback: (() -> Unit)? = null
    var fileName: String = asset.file?.name.orEmpty()
    var fileSize: String = StringUtil.formatFileSize(asset.file?.length ?: 0)

    val designSystemTypes: Array<DesignSystemType>
        get() = DesignSystemType.selectableTypes()

    fun selectDesignSystemType(designSystemType: DesignSystemType) {
        updateDesignSystemTypeCallback(designSystemType)
    }

    fun updateSampleCode(text: String) {
        updateSampleCodeCallback(text)
    }

    fun removeFile() {
        removeCallback()
    }

    fun validateText(text: String?, textArea: JTextArea): ValidationInfo? {
        return if (text.isNullOrEmpty()) {
            ValidationInfo("Cannot be empty", textArea)
        } else {
            null
        }
    }
}