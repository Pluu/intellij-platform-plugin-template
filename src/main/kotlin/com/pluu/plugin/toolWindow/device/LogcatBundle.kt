package com.pluu.plugin.toolWindow.device

import com.android.tools.idea.localization.MessageBundleReference
import org.jetbrains.annotations.PropertyKey

internal const val BUNDLE_NAME = "messages.LogcatBundle"

/** Message bundle for the logcat module. */
internal object LogcatBundle {
  private val bundleRef = MessageBundleReference(BUNDLE_NAME)

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String =
    bundleRef.message(key, *params)
}
