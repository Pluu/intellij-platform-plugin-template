package com.pluu.plugin.settings

import com.android.annotations.concurrency.UiThread
import com.intellij.util.messages.Topic
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import java.util.*

interface ConfigSettingsListener : EventListener {

    companion object {
        val TOPIC: Topic<ConfigSettingsListener> =
            Topic.create("Config settings", ConfigSettingsListener::class.java)
    }

    @UiThread
    fun onEnableChanged(isEnable: Boolean)

    @UiThread
    fun onDesignSystemTypeChanged(list: List<DesignSystemType>)


}
