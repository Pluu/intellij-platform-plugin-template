package com.pluu.plugin.toolWindow.designsystem.explorer.action

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/actions/HeaderAction.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/** Disabled action for dropdowns and popups which represent header. */
class HeaderAction(text: String?, description: String?) : AnAction(text, description, null) {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = false
  }

  override fun actionPerformed(e: AnActionEvent) = Unit
}