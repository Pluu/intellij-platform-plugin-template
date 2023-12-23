package com.pluu.plugin.toolWindow.designsystem.provider

import com.android.annotations.concurrency.WorkerThread
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet

object MyViewFindModel {
    @WorkerThread
    fun findDesignKit(forFacet: AndroidFacet, type: DesignSystemType): List<DesignSystemItem> {
        val project = forFacet.module.project.getBaseDirectories()
            .firstOrNull()
            ?.findChild("pluu")
            ?.findChild(type.name.lowercase())
            ?.takeIf { it.isDirectory }
            ?: return emptyList()
        return project.children.map {
            DesignSystemItem(
                type = type,
                name = it.nameWithoutExtension,
                virtualFile = it
            )
        }
    }
}