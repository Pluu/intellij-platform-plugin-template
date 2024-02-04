package com.pluu.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType

@State(
    name = "ConfigSettings",
    storages = [Storage("PluuPlugin.xml")]
)
class ConfigSettings : SimplePersistentStateComponent<ConfigSettings.State>(State()) {

    class State : BaseState() {
        var isDesignSystemEnable: Boolean by property(true)

        var types by list<String>()
    }

    var isDesignSystemEnable: Boolean
        get() = state.isDesignSystemEnable
        set(value) {
            state.isDesignSystemEnable = value
            notifyListeners {
                onEnableChanged(value)
            }
        }

    private var initialized = false

    override fun initializeComponent() {
        initialized = true
        if (state.types.isEmpty()) {
            state.types.addAll(defaultDesignSystemType)
        }
    }

    fun setTypes(types: List<DesignSystemType>) {
        state.types.clear()
        state.types.addAll(types.map { it.name })
        notifyListeners {
            onDesignSystemTypeChanged(types)
        }
    }

    fun getTypes(): List<DesignSystemType> = state.types
        .map { DesignSystemType(it) }

    private fun notifyListeners(action: ConfigSettingsListener.() -> Unit) {
        // Notify listeners if this is the main ConfigSettings instance, and it has been already initialized.
        if (initialized && this == getInstance()) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(ConfigSettingsListener.TOPIC)
                .action()
        }
    }

    companion object {
        private val defaultDesignSystemType: List<String> by lazy {
            listOf("Input", "Button", "Toast", "Chips")
        }

        fun getInstance(): ConfigSettings =
            ApplicationManager.getApplication().service<ConfigSettings>()
    }
}