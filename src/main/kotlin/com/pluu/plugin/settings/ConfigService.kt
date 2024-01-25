package com.pluu.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "PluuPlugin",
    storages = [Storage("PluuPlugin.xml")]
)
class ConfigService : PersistentStateComponent<Config> {
    private val config = Config

    override fun getState(): Config = config

    override fun loadState(state: Config) {
        XmlSerializerUtil.copyBean(state, this.config)
    }
}