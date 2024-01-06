package com.pluu.plugin.toolWindow.designsystem.explorer.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.Icon

/**
 * Button to add new resources
 */
abstract class PopupAction(
    val icon: Icon?,
    description: String
) : AnAction(description, description, icon), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        var x = 0
        var y = 0
        val inputEvent = e.inputEvent
        if (inputEvent is MouseEvent) {
            x = 0
            y = inputEvent.component.height
        }

        showAddPopup(inputEvent.component, x, y)
    }

    private fun showAddPopup(component: Component, x: Int, y: Int) {
        ActionManager.getInstance()
            .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, createAddPopupGroup())
            .component.show(component, x, y)
    }

    protected abstract fun createAddPopupGroup(): ActionGroup
}