package com.pluu.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
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

    companion object {
        fun getInstance(project: Project): ConfigService = project.service()
    }
}