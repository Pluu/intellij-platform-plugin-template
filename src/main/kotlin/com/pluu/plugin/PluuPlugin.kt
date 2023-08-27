package com.pluu.plugin

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object PluuPlugin {
    val PLUU_ICON: Icon
        get() = IconLoader.getIcon("assets/androidstudio_icon.svg", PluuPlugin::class.java)
}