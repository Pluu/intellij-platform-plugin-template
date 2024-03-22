package com.pluu.plugin.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class ConfigConfigurable(
    private val project: Project
) : SearchableConfigurable {

    private val configSettings = ConfigSettings.getInstance()
    private val configProjectSettings = ConfigProjectSettings.getInstance(project)

    private var configComponent: ConfigComponent? = null

    override fun createComponent(): JComponent? {
        configComponent = ConfigComponent(project, configSettings, configProjectSettings)
        return configComponent?.root
    }

    override fun disposeUIResources() {
        configComponent = null
    }

    override fun isModified(): Boolean {
        return requireNotNull(configComponent).isModified()
    }

    override fun apply() {
        requireNotNull(configComponent).apply()
    }

    override fun reset() {
        requireNotNull(configComponent).reset()
    }

    override fun getDisplayName(): String = "Pluu Plugin"

    override fun getId(): String = "com.pluu.plugin.settings.ConfigConfigurable"
}