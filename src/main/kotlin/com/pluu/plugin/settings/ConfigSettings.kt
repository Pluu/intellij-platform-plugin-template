package com.pluu.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlin.reflect.KProperty

@Service
@State(name = "PluuPlugin", storages = [Storage("PluuPlugin.xml")])
class ConfigSettings : PersistentStateComponent<ConfigSettings> {

    var isDesignSystemEnable: Boolean by ChangeNotifyingProperty(true)

    private var initialized = false

    override fun getState(): ConfigSettings = this

    override fun loadState(state: ConfigSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun initializeComponent() {
        initialized = true
    }

    private fun notifyListeners() {
        // Notify listeners if this is the main ConfigSettings instance, and it has been already initialized.
        if (initialized && this == getInstance()) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(ConfigSettingsListener.TOPIC)
                .settingsChanged(this)
        }
    }

    companion object {
        fun getInstance(): ConfigSettings =
            ApplicationManager.getApplication().service<ConfigSettings>()
    }

    private inner class ChangeNotifyingProperty<T>(var value: T) {
        operator fun getValue(thisRef: ConfigSettings, property: KProperty<*>) = value
        operator fun setValue(thisRef: ConfigSettings, property: KProperty<*>, newValue: T) {
            if (value != newValue) {
                value = newValue
                notifyListeners()
            }
        }
    }
}