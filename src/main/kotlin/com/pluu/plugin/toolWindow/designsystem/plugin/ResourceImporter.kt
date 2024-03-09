package com.pluu.plugin.toolWindow.designsystem.plugin

///////////////////////////////////////////////////////////////////////////
// Origin: https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/plugin/ResourceImporter.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.io.File
import javax.imageio.ImageIO

// Plugin interface to add resources importation plugins.
class ResourceImporter {
    /**
     * Returns a list of the file extensions supported by this plugin.
     */
    fun getSupportedFileTypes(): Set<String> {
        return imageTypeExtensions
    }

    fun processFile(file: File): DesignSystemItem? {
        val virtualFile = VfsUtil.findFileByIoFile(file, true) ?: return null
        return processFile(virtualFile)
    }

    fun processFile(file: VirtualFile): DesignSystemItem {
        return DesignSystemItem(
            type = DesignSystemType.NONE,
            name = file.nameWithoutExtension,
            aliasName = null,
            file = file,
            applicableFileType = ApplicableFileType.NONE,
            sampleCode = null
        )
    }

    companion object {
        private val imageTypeExtensions = ImageIO.getReaderFormatNames().toSet()
    }
}