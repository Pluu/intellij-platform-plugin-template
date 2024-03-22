package com.pluu.plugin.toolWindow.designsystem.importer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/importer/ResourceImportManager.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.ui.resourcemanager.importer.getAllLeafFiles
import com.android.tools.idea.util.toIoFile
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.io.File
import java.nio.file.InvalidPathException
import javax.swing.JComponent

/**
 * Constant used in the file chooser to save the last used location and re-open
 * it the next time the chooser is used.
 */
private const val PREFERENCE_LAST_SELECTED_DIRECTORY = "designSystemExplorer.lastChosenDirectory"

/**
 * Recursively find all files that can be converted to [DesignSystemItem] in hierarchies
 * of this [Sequence]'s files.
 *
 * The conversion is done by the first [com.pluu.plugin.toolWindow.designsystem.plugin.ResourceImporter]
 * provided by [importersProvider] and compatible with a given file.
 */
fun Sequence<File>.findAllDesignAssets(importersProvider: ImportersProvider): Sequence<DesignSystemItem> =
    flatMap {
        it.getAllLeafFiles()
    }.toDesignAsset(importersProvider)

/**
 * Transforms this [File] [Sequence] into a [DesignSystemItem] [Sequence] using the available.
 * [File]s that couldn't be converted into a [DesignSystemItem] are silently ignored.
 *
 * @see ImportersProvider
 * @see ImportersProvider.getImportersForExtension
 * @see com.pluu.plugin.toolWindow.designsystem.plugin.ResourceImporter.processFile
 */
fun Sequence<File>.toDesignAsset(importersProvider: ImportersProvider): Sequence<DesignSystemItem> =
    mapNotNull {
        importersProvider.getImportersForExtension(it.extension)?.processFile(it)
    }

/**
 * Group [DesignSystemItem]s by their name into [DesignAssetSet].
 */
fun Sequence<DesignSystemItem>.groupIntoDesignAssetSet(): List<DesignAssetSet> =
    map {
        DesignAssetSet(it.name, it)
    }.sortedBy { it.name }
        .toList()

/**
 * Displays a file picker which filters files depending on the files supported by the [DesignAssetImporter]
 * provided by the [importersProvider]. When files have been chosen, the [fileChosenCallback] is invoked with
 * the files converted into DesignAssetSet.
 */
fun chooseDesignAssets(
    importersProvider: ImportersProvider,
    parent: JComponent? = null,
    fileChosenCallback: (Sequence<DesignSystemItem>) -> Unit
) {
    val lastChosenDirFile: VirtualFile? = PropertiesComponent.getInstance()
        .getValue(PREFERENCE_LAST_SELECTED_DIRECTORY)
        ?.let {
            try {
                VfsUtil.findFile(File(it).toPath(), true)
            } catch (ex: InvalidPathException) {
                null
            }
        }
    val fileChooserDescriptor = createFileDescriptor(importersProvider)
    FileChooser.chooseFiles(fileChooserDescriptor, null, parent, lastChosenDirFile) { selectedFiles ->
        val allDesignAssets = selectedFiles.asSequence()
            .map { it.toIoFile() }
            .findAllDesignAssets(importersProvider)
        fileChosenCallback(allDesignAssets)

        // 마지막 로드한 위치 저장
        PropertiesComponent.getInstance()
            .setValue(PREFERENCE_LAST_SELECTED_DIRECTORY, selectedFiles.firstOrNull()?.path)
    }
}

/**
 * Create a [FileChooserDescriptor] from the available [DesignAssetImporter] provided by the ImportersProvider.
 */
private fun createFileDescriptor(importersProvider: ImportersProvider): FileChooserDescriptor {
    val supportedFileTypes = importersProvider.supportedFileTypes
    return FileChooserDescriptor(true, true, false, false, false, true)
        .withFileFilter { it.extension in supportedFileTypes }
}