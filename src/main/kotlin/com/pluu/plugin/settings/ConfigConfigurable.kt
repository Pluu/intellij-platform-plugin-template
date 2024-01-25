package com.pluu.plugin.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class ConfigConfigurable : Configurable {
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
        return Config.isDesignSystemEnable != form.isEnableDesignSystem
    }

    override fun apply() {
        val form = requireNotNull(configComponent)
        Config.isDesignSystemEnable = form.isEnableDesignSystem
    }

    override fun reset() {
        val form = requireNotNull(configComponent)
        form.isEnableDesignSystem = Config.isDesignSystemEnable
    }

    override fun getDisplayName(): String = "Pluu Plugin"
}