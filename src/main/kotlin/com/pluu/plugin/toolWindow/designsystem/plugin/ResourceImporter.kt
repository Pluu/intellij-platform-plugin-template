package com.pluu.plugin.toolWindow.designsystem.plugin

import com.intellij.openapi.vfs.VfsUtil
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.io.File
import javax.imageio.ImageIO

class ResourceImporter {
    /**
     * Returns a list of the file extensions supported by this plugin.
     */
    fun getSupportedFileTypes(): Set<String> {
        return imageTypeExtensions
    }

    fun processFile(file: File): DesignSystemItem? {
        val virtualFile = VfsUtil.findFileByIoFile(file, true) ?: return null
        return DesignSystemItem(
            DesignSystemType.BUTTON,
            virtualFile.nameWithoutExtension,
            virtualFile,
            null
        )
    }

    companion object {
        private val imageTypeExtensions = ImageIO.getReaderFormatNames().toSet()
    }
}