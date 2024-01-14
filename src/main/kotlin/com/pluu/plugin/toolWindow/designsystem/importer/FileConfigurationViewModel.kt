package com.pluu.plugin.toolWindow.designsystem.importer

import java.util.*
import kotlin.properties.Delegates

class FileConfigurationViewModel {

    var onConfigurationUpdated: ((List<String>) -> Unit)? = null

    private val usedAliasName = mutableListOf<AliasConfigParam>()

    private var incrementalIndex = 0

    fun newConfigKey(): AliasConfigParam {
        val param = AliasConfigParam(
            incrementalIndex++
        ) {
            it.isNotEmpty()
        }
        usedAliasName.add(param)
        return param
    }

    fun applyConfiguration() {
        onConfigurationUpdated?.invoke(
            usedAliasName
                .mapNotNull { it.paramValue }
                .filter { it.isNotEmpty() }
                .toList()
        )
    }
}

data class AliasConfigParam(
    val index: Int,
    val validator: (String) -> Boolean
) : Observable() {
    var paramValue: String? by Delegates.observable(null) { _, old, new ->
        if (new != old) {
            setChanged()
        }
        notifyObservers(new)
    }
}