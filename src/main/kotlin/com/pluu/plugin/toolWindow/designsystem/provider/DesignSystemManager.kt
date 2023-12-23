package com.pluu.plugin.toolWindow.designsystem.provider

import com.android.annotations.concurrency.WorkerThread
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
        val project = forFacet.module.project.getBaseDirectories()
            .firstOrNull()
            ?.findChild("pluu")
            ?.findChild(type.name.lowercase())
            ?.takeIf { it.isDirectory }
            ?: return emptyList()
        return project.children.asSequence()
            .filter { supportImageExtension.contains(it.extension) }
            .map {
                DesignSystemItem(
                    type = type,
                    name = it.nameWithoutExtension,
                    file = it
                )
            }.sortedBy { it.name }
            .toList()
    }
}