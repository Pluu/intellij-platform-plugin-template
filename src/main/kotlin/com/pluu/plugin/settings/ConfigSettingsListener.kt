package com.pluu.plugin.settings

import com.android.annotations.concurrency.UiThread
import com.intellij.util.messages.Topic
import java.util.*

fun interface ConfigSettingsListener : EventListener {

    companion object {
        val TOPIC: Topic<ConfigSettingsListener> =
            Topic.create("Config settings", ConfigSettingsListener::class.java)
    }

    @UiThread
    fun settingsChanged(settings: ConfigSettings)
}
