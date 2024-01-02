package com.pluu.plugin.toolWindow.designsystem.provider

import com.android.annotations.concurrency.WorkerThread
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet
import javax.imageio.ImageIO

object DesignSystemManager {

    // https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/plugin/RasterAssetRenderer.kt
    private val supportImageExtension by lazy {
        ImageIO.getReaderFormatNames()
    }

    @WorkerThread
    fun findDesignKit(forFacet: AndroidFacet, type: DesignSystemType): List<DesignSystemItem> {
        val rootPath = forFacet.module.project.getBaseDirectories()
            .firstOrNull()
            ?.findChild("pluu") ?: return emptyList()

        val jsonObject = rootPath.findChild("sample.json")
            ?.let {
                JsonParser.parseReader(it.inputStream.reader(Charsets.UTF_8)) as? JsonObject
            } ?: return emptyList()

        return jsonObject.getAsJsonArray(type.name.lowercase())
            .map { it.asJsonObject }
            .map {
                DesignSystemItem(
                    type = type,
                    name = it.get("id").asString,
                    file = rootPath.findChild(type.name.lowercase())?.findChild(it.get("thumbnail").asString),
                    sampleCode = it.get("code").asString
                )
            }
    }
}