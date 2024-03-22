package com.pluu.plugin.toolWindow.designsystem.importer

import com.intellij.openapi.project.Project
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.provider.DesignSystemManager

class DesignSystemDataValidator private constructor(
    private val existing: List<DesignSystemItem>
) {
    fun isExist(type: DesignSystemType, name: String, skipName: String?): Boolean {
        return existing.any {
            it.type == type && it.name != skipName && it.name == name
        }
    }

    companion object {
        fun forSamples(project: Project): DesignSystemDataValidator {
            return DesignSystemDataValidator(
                DesignSystemManager.getDesignSystemResources(project, null)
            )
        }
    }
}