package com.pluu.plugin.module

import com.intellij.ide.util.PropertiesComponent

// this is used both by new project and new module UI
internal const val PROPERTIES_BYTECODE_LEVEL_KEY = "SAVED_BYTECODE_LEVEL"

internal val properties get() = PropertiesComponent.getInstance()
