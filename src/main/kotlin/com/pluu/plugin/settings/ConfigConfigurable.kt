package com.pluu.plugin.settings

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class ConfigConfigurable : SearchableConfigurable {
    private var configComponent: ConfigComponent? = null

    override fun createComponent(): JComponent? {
        configComponent = ConfigComponent().also {
            it.setDesignSystemTypes(ConfigSettings.getInstance().getTypes())
        }
        return configComponent?.root
    }

    override fun disposeUIResources() {
        configComponent = null
    }

    override fun isModified(): Boolean {
        val form = requireNotNull(configComponent)
        val configSettings = ConfigSettings.getInstance()
        return configSettings.isDesignSystemEnable != form.isEnableDesignSystem ||
                configSettings.getTypes().joinToString() != form.designSystemTypes().joinToString()
    }

    override fun apply() {
        val form = requireNotNull(configComponent)
        val configSettings = ConfigSettings.getInstance()
        configSettings.isDesignSystemEnable = form.isEnableDesignSystem
        configSettings.setTypes(form.designSystemTypes())
    }

    override fun reset() {
        val form = requireNotNull(configComponent)
        form.isEnableDesignSystem = ConfigSettings.getInstance().isDesignSystemEnable
    }

    override fun getDisplayName(): String = "Pluu Plugin"

    override fun getId(): String = "com.pluu.plugin.settings.ConfigConfigurable"
}