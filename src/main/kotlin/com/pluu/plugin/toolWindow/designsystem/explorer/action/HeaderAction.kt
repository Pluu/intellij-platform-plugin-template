package com.pluu.plugin.toolWindow.designsystem.explorer.action

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