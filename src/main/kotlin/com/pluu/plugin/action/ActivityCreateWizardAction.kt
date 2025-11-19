package com.pluu.plugin.action

import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.pluu.plugin.action.component.UiComponentCreateWizardAction
import com.pluu.plugin.wizard.activity.sampleActivitySetupTemplate

class ActivityCreateWizardAction : UiComponentCreateWizardAction(
    wizardUiContext = AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.ACTIVITY_GALLERY,
    templateProvider = {
        sampleActivitySetupTemplate
    },
    dialogTitle = "android.wizard.new.activity.title",
    stepTitle = "android.wizard.config.activity.title"
)