package com.pluu.plugin.settings

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class ConfigConfigurable : SearchableConfigurable {
    private var configComponent: ConfigComponent? = null

    override fun createComponent(): JComponent? {
        configComponent = ConfigComponent()
        return configComponent?.root
    }

    override fun disposeUIResources() {
        configComponent = null
    }

    override fun isModified(): Boolean {
        val form = requireNotNull(configComponent)
        return ConfigSettings.getInstance().isDesignSystemEnable != form.isEnableDesignSystem
    }

    override fun apply() {
        val form = requireNotNull(configComponent)
        ConfigSettings.getInstance().isDesignSystemEnable = form.isEnableDesignSystem
    }

    override fun reset() {
        val form = requireNotNull(configComponent)
        form.isEnableDesignSystem = ConfigSettings.getInstance().isDesignSystemEnable
    }

    override fun getDisplayName(): String = "Pluu Plugin"

    override fun getId(): String = "com.pluu.plugin.settings.ConfigConfigurable"
}