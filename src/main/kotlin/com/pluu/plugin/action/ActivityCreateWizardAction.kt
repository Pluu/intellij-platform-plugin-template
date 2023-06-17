package com.pluu.plugin.action

import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.action.component.UiComponentCreateWizardAction
import icons.StudioIcons

class ActivityCreateWizardAction : UiComponentCreateWizardAction(
    text = "Create Activity",
    wizardUiContext = AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.ACTIVITY_GALLERY,
    templateName = PluuPlugin.ACTIVITY_WITH_VIEWMODEL,
    dialogTitle = "android.wizard.new.activity.title",
    stepTitle = "android.wizard.config.activity.title"
) {
    init {
        templatePresentation.icon = StudioIcons.Shell.Filetree.ACTIVITY
    }
}