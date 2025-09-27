package com.pluu.plugin.action

import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.pluu.plugin.PluuBundle
import com.pluu.plugin.action.component.UiComponentCreateWizardAction

class ActivityCreateWizardAction : UiComponentCreateWizardAction(
    wizardUiContext = AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.ACTIVITY_GALLERY,
    templateName = PluuBundle.message("pluu.activity.and.viewmodel.new.feature.title"),
    dialogTitle = "android.wizard.new.activity.title",
    stepTitle = "android.wizard.config.activity.title"
)