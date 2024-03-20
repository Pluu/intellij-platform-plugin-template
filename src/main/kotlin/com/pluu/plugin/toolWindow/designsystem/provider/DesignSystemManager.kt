package com.pluu.plugin.toolWindow.designsystem.provider

import com.android.annotations.concurrency.Slow
import com.android.annotations.concurrency.WorkerThread
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.settings.ConfigProjectSettings
import com.pluu.plugin.settings.ConfigSettings
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.DesignAssetSet
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import java.io.File
import java.nio.file.Files

object DesignSystemManager {

    private const val sampleJsonFileName = "sample.json"

    /**
     * Returns a list of local design systems.
     */
    @Slow
    fun getDesignSystemResources(
        project: Project,
        type: DesignSystemType?
    ): List<DesignSystemItem> {
        val types = if (type != null) {
            listOf(type)
        } else {
            ConfigSettings.getInstance().getTypes()
        }
        return findDesignKit(project, types)
    }

    private fun sampleRootDirectoryPath(project: Project): String? {
        val configSetting = ConfigProjectSettings.getInstance(project)
        return configSetting.sampleRootDirectory
    }

    private fun sampleRootDirectory(project: Project): VirtualFile? {
        return sampleRootDirectoryPath(project)?.let { path ->
            File(path).toVirtualFile()
        }
    }

    fun getOrCreateDefaultRootDirectory(project: Project): VirtualFile {
        val rootFile = sampleRootDirectory(project)
        if (rootFile != null) return rootFile

        WriteCommandAction.runWriteCommandAction(project, "Write Sample Root", null, {
            val sampleRootPath = requireNotNull(sampleRootDirectoryPath(project))
            VfsUtil.createDirectoryIfMissing(sampleRootPath)
        })

        return requireNotNull(sampleRootDirectory(project))
    }

    fun saveSample(project: Project, type: DesignSystemType, items: List<DesignAssetSet>): Boolean {
        return edit(project) { jsonObject ->
            val j = jsonObject.getAsJsonArray(type.jsonKey) ?: JsonArray().also {
                jsonObject.add(type.jsonKey, it)
            }
            items.forEach { assetSet ->
                j.add(assetSet.asJson())
            }
            true
        }
    }

    fun removeSample(project: Project, type: DesignSystemType, item: DesignSystemItem): Boolean {
        return edit(project) { jsonObject ->
            val j = jsonObject.getAsJsonArray(type.jsonKey) ?: return@edit false
            val index = j.indexOfFirst {
                it.asJsonObject.get("id").asString == item.name
            }.takeIf { it >= 0 } ?: return@edit false

            j.remove(index)
            true
        }
    }

    private fun edit(project: Project, action: (JsonObject) -> Boolean): Boolean {
        val jsonObject = loadJsonFromSampleFile(project, true)

        action(jsonObject)

        val gson = GsonBuilder().setLenient().setPrettyPrinting()
            .create()

        WriteCommandAction.runWriteCommandAction(project, "Write Json", null, {
            sampleRootDirectory(project)
                ?.findChild(sampleJsonFileName)
                ?.let {
                    Files.newBufferedWriter(it.toNioPath(), Charsets.UTF_8).use { writer ->
                        gson.toJson(jsonObject, writer)
                    }
                }
        })
        return true
    }

    private fun loadJsonFromSampleFile(project: Project, isRequiredFile: Boolean): JsonObject {
        val rootPath = getOrCreateDefaultRootDirectory(project)
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
    fun findDesignKit(project: Project, types: List<DesignSystemType>): List<DesignSystemItem> {
        val rootPath = sampleRootDirectory(project) ?: return emptyList()
        LocalFileSystem.getInstance().refreshFiles(listOf(rootPath))

        val jsonObject = loadJsonFromSampleFile(project, false)

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