package com.pluu.plugin.toolWindow.designsystem.importer

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/importer/ImportersProvider.kt
///////////////////////////////////////////////////////////////////////////

import com.pluu.plugin.toolWindow.designsystem.plugin.ResourceImporter

class ImportersProvider(
    private val importer: ResourceImporter = ResourceImporter()
) {
    /**
     * Returns the all the file extension supported by the available plugins
     */
    val supportedFileTypes = importer.getSupportedFileTypes()

    /**
     * Returns a list of [ResourceImporter] that supports the provided extension.
     */
    fun getImportersForExtension(extension: String): ResourceImporter? {
        if (supportedFileTypes.contains(extension)) {
            return importer
        }
        return null
    }
}