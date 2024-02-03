package com.pluu.plugin.toolWindow.designsystem.provider

import com.android.annotations.concurrency.Slow
import com.android.annotations.concurrency.WorkerThread
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.settings.ConfigSettings
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet
import java.nio.file.Files

object DesignSystemManager {

    private const val sampleDirName = "pluu"

    private const val sampleJsonFileName = "sample.json"

    /**
     * Returns a list of local design systems.
     */
    @Slow
    fun getModuleResources(
        facet: AndroidFacet,
        type: DesignSystemType?
    ): List<DesignSystemItem> {
        val types = if (type != null) {
            listOf(type)
        } else {
            ConfigSettings.getInstance().getTypes()
        }
        return findDesignKit(facet, types)
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

    fun saveSample(facet: AndroidFacet, type: DesignSystemType, items: List<DesignAssetSet>): Boolean {
        return edit(facet) { jsonObject ->
            val j = jsonObject.getAsJsonArray(type.jsonKey) ?: JsonArray().also {
                jsonObject.add(type.jsonKey, it)
            }
            items.forEach { assetSet ->
                j.add(assetSet.asJson())
            }
            true
        }
    }

    fun removeSample(facet: AndroidFacet, type: DesignSystemType, item: DesignSystemItem): Boolean {
        return edit(facet) { jsonObject ->
            val j = jsonObject.getAsJsonArray(type.jsonKey) ?: return@edit false
            val index = j.indexOfFirst {
                it.asJsonObject.get("id").asString == item.name
            }.takeIf { it >= 0 } ?: return@edit false

            j.remove(index)
            true
        }
    }

    private fun edit(facet: AndroidFacet, action: (JsonObject) -> Boolean): Boolean {
        val project = facet.module.project
        val jsonObject = loadJsonFromSampleFile(facet, true)

        action(jsonObject)

        val gson = GsonBuilder().setLenient().setPrettyPrinting()
            .create()

        WriteCommandAction.runWriteCommandAction(project, "Write Json", null, {
            rootPath(facet)
                ?.findChild(sampleJsonFileName)
                ?.let {
                    Files.newBufferedWriter(it.toNioPath(), Charsets.UTF_8).use { writer ->
                        gson.toJson(jsonObject, writer)
                    }
                }
        })
        return true
    }

    private fun loadJsonFromSampleFile(facet: AndroidFacet, isRequiredFile: Boolean): JsonObject {
        val rootPath = getOrCreateDefaultRootDirectory(facet)
        LocalFileSystem.getInstance().refreshFiles(listOf(rootPath))

        return rootPath.findChild(sampleJsonFileName)
            ?.let {
                JsonParser.parseReader(it.inputStream.reader(Charsets.UTF_8)) as? JsonObject
            } ?: run {
            if (isRequiredFile) {
                VfsUtil.createChildSequent(this, rootPath, "sample", "json")
            }
            JsonObject()
        }
    }

    @WorkerThread
    fun findDesignKit(facet: AndroidFacet, types: List<DesignSystemType>): List<DesignSystemItem> {
        val rootPath = rootPath(facet) ?: return emptyList()
        LocalFileSystem.getInstance().refreshFiles(listOf(rootPath))

        val jsonObject = loadJsonFromSampleFile(facet, false)

        return types.flatMap { type ->
            jsonObject.getAsJsonArray(type.jsonKey)
                ?.map { it.asJsonObject }
                .orEmpty()
                .map {
                    it.asDesignSystemItem(rootPath, type)
                }
        }
    }

    private fun JsonObject.asDesignSystemItem(rootPath: VirtualFile, type: DesignSystemType): DesignSystemItem {
        return DesignSystemItem(
            type = type,
            name = get("id").asString,
            file = rootPath.findChild(type.sampleDirName)?.findChild(get("thumbnail").asString),
            aliasNames = getAsJsonArray("alias")?.map { it.asString },
            applicableFileType = ApplicableFileType.of(get("applicableFileType").asString),
            sampleCode = get("code").asString
        )
    }

    private fun DesignAssetSet.asJson(): JsonObject {
        val json = JsonObject()
        json.addProperty("id", asset.name)
        json.addProperty("thumbnail", "${name}.${asset.file!!.extension}")

        val alisNames = JsonArray()
        asset.aliasNames?.forEach {
            alisNames.add(it)
        }
        if (!alisNames.isEmpty) {
            json.add("alias", alisNames)
        }

        json.addProperty("applicableFileType", asset.applicableFileType.name)
        json.addProperty("code", asset.sampleCode)
        return json
    }
}

val DesignSystemType.sampleDirName: String
    get() = name.lowercase()

val DesignSystemType.jsonKey: String
    get() = name.lowercase()