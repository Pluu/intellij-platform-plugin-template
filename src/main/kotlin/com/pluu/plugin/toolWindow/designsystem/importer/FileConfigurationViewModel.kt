package com.pluu.plugin.toolWindow.designsystem.importer

import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.util.*
import kotlin.properties.Delegates

class FileConfigurationViewModel(
    list: List<AliasConfigParam> = emptyList()
) {
    constructor(assets: Sequence<DesignSystemItem>) :
            this(
                assets.flatMap { it.aliasNames.orEmpty() }
                    .mapIndexed { index, s ->
                        AliasConfigParam(index) {
                            it.isNotEmpty()
                        }.apply {
                            paramValue = s
                        }
                    }.toList()
            )

    var onConfigurationUpdated: ((List<String>) -> Unit)? = null

    private val usedAliasName = mutableListOf(*list.toTypedArray())

    fun getCurrentAliasName(): List<AliasConfigParam> = usedAliasName

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

    fun deleteAlias(param: AliasConfigParam) {
        usedAliasName.remove(param)
        applyConfiguration()
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