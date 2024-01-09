package com.pluu.plugin.toolWindow.designsystem.provider

import com.android.annotations.concurrency.WorkerThread
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet
import javax.imageio.ImageIO

object DesignSystemManager {

    private val sampleDirName = "pluu"

    // https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/plugin/RasterAssetRenderer.kt
    private val supportImageExtension by lazy {
        ImageIO.getReaderFormatNames()
    }

    private fun rootPath(facet: AndroidFacet): VirtualFile? {
        return facet.module.project.guessProjectDir()
            ?.findChild(sampleDirName)
    }

    fun getOrCreateDefaultRootDirectory(facet: AndroidFacet): VirtualFile {
        val rootFile = rootPath(facet)
        if (rootFile != null) return rootFile

        val project = facet.module.project
        WriteCommandAction.runWriteCommandAction(project, "Write Sample Root", null, {
            VfsUtil.createDirectoryIfMissing(project.guessProjectDir(), sampleDirName)
        })

        return requireNotNull(rootPath(facet))
    }

    @WorkerThread
    fun findDesignKit(facet: AndroidFacet, type: DesignSystemType): List<DesignSystemItem> {
        val rootPath = rootPath(facet) ?: return emptyList()

        val jsonObject = rootPath.findChild("sample.json")
            ?.let {
                JsonParser.parseReader(it.inputStream.reader(Charsets.UTF_8)) as? JsonObject
            } ?: return emptyList()

        return jsonObject.getAsJsonArray(type.jsonKey)
            ?.map { it.asJsonObject }
            .orEmpty()
            .map {
                DesignSystemItem(
                    type = type,
                    name = it.get("id").asString,
                    file = rootPath.findChild(type.sampleDirName)?.findChild(it.get("thumbnail").asString),
                    sampleCode = it.get("code").asString
                )
            }
    }
}

val DesignSystemType.sampleDirName: String
    get() = name.lowercase()

val DesignSystemType.jsonKey: String
    get() = name.lowercase()