package com.pluu.plugin.toolWindow.designsystem.importer

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.pluu.plugin.settings.ConfigSettings
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import javax.swing.JTextArea

class FileImportRowViewModel(
    asset: DesignSystemItem,
    val updateDesignSystemTypeCallback: (DesignSystemType) -> Unit,
    val updateSampleCodeCallback: (String) -> Unit,
    val updateApplicableFileTypeCallback: (ApplicableFileType) -> Unit,
    val removeCallback: () -> Unit
) {
    // TODO get value from actual file
    var updateCallback: (() -> Unit)? = null
    var fileName: String = asset.file?.name.orEmpty()
    var fileSize: String = StringUtil.formatFileSize(asset.file?.length ?: 0)
    var sampleCode: String = asset.sampleCode.orEmpty()
    var designSystemType: DesignSystemType? = asset.type.takeIf { it.isSelectable() }
    var applicableFileType: ApplicableFileType? = asset.applicableFileType.takeIf { it.isSelectable() }

    val selectableDesignSystemTypes: List<DesignSystemType>
        get() = ConfigSettings.getInstance().state.types

    fun selectDesignSystemType(designSystemType: DesignSystemType) {
        if (this.designSystemType == designSystemType) return
        this.designSystemType = designSystemType
        updateDesignSystemTypeCallback(designSystemType)
    }

    val selectableApplicableFile: Array<ApplicableFileType>
        get() = ApplicableFileType.selectableTypes()

    fun selectApplicableFile(type: ApplicableFileType) {
        if (this.applicableFileType == type) return
        this.applicableFileType = type
        updateApplicableFileTypeCallback(type)
    }

    fun updateSampleCode(text: String) {
        if (this.sampleCode == text) return
        this.sampleCode = text
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