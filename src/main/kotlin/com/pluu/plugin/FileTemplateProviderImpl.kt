package com.pluu.plugin

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import icons.StudioIcons

class FileTemplateProviderImpl : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Pluu", StudioIcons.Common.ANDROID_HEAD)
        group.addTemplate(
            FileTemplateDescriptor(PLUU_VIEW_MODEL, StudioIcons.Shell.Filetree.ANDROID_FILE)
        )
        return group
    }

    companion object {
        const val PLUU_VIEW_MODEL = "PluuViewModel.kt"
    }
}