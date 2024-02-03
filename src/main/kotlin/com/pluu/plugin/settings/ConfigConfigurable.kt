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
        val form = requireNotNull(configComponent)
        return form.isModified()
    }

    override fun apply() {
        val form = requireNotNull(configComponent)
        form.apply()
    }

    override fun reset() {
        val form = requireNotNull(configComponent)
        form.reset()
    }

    override fun getDisplayName(): String = "Pluu Plugin"

    override fun getId(): String = "com.pluu.plugin.settings.ConfigConfigurable"
}