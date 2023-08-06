package com.pluu.plugin

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object PluuPlugin {
    const val ACTIVITY_WITH_VIEWMODEL = "[Pluu] Activity with ViewModel"
    const val FRAGMENT_WITH_VIEWMODEL = "[Pluu] Fragment with ViewModel"
    const val PLUU_MODULE = "[Pluu] Android Library Module"

    object TestAction {
        const val CreateViewModel = "Create ViewModelTest"
        const val CreateActivity = "Create ActivityTest"
    }

    val PLUU_ICON: Icon
        get() = IconLoader.getIcon("assets/androidstudio_icon.svg", PluuPlugin::class.java)
}