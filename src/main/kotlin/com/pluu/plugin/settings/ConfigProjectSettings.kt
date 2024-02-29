package com.pluu.plugin.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

@Service(Service.Level.PROJECT)
@State(
    name = "PluuPlugin_ConfigProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ConfigProjectSettings(
    private val project: Project
) : SimplePersistentStateComponent<ConfigProjectSettings.State>(State()) {

    class State : BaseState() {
        var sampleRootDirectory by string()
    }

    var sampleRootDirectory: String? by ValueWithDefault(state::sampleRootDirectory) { defaultSampleRootDirectory }

    val defaultSampleRootDirectory: String?
        get() {
            val directory = project.guessProjectDir()?.canonicalPath
            return if (directory != null) {
                File(directory).resolve(Project.DIRECTORY_STORE_FOLDER + "/${sampleDirName}").canonicalPath
            } else {
                null
            }
        }

    companion object {
        private const val sampleDirName = "pluu"

        @JvmStatic
        fun getInstance(project: Project): ConfigProjectSettings {
            return project.getService(ConfigProjectSettings::class.java)
        }
    }
}

class ValueWithDefault<T : String?>(
    private val prop: KMutableProperty0<T?>,
    val default: () -> T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val value: T? = prop.get()
        return if (value !== null) value else default()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        prop.set(if (value == default() || value.isNullOrEmpty()) null else value)
    }
}