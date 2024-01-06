package com.pluu.plugin.toolWindow.designsystem.importer

import com.pluu.plugin.toolWindow.designsystem.plugin.ResourceImporter

class ImportersProvider(
    val importer: ResourceImporter = ResourceImporter()
) {

    private val typeToImporter = importer.getSupportedFileTypes().map { Pair(it, importer) }.toList()
        .groupBy({ it.first }, { it.second })


    /**
     * Returns the all the file extension supported by the available plugins
     */
    val supportedFileTypes = importer.getSupportedFileTypes()

    /**
     * Returns a list of [ResourceImporter] that supports the provided extension.
     */
    fun getImportersForExtension(extension: String): List<ResourceImporter> {
        val importers = typeToImporter[extension] ?: emptyList()
        return importers
    }
}