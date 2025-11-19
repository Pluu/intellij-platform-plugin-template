package com.pluu.plugin.action

import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.FRAGMENT_GALLERY
import com.pluu.plugin.action.component.UiComponentCreateWizardAction
import com.pluu.plugin.wizard.fragment.sampleFragmentSetupTemplate

class FragmentCreateWizardAction : UiComponentCreateWizardAction(
    wizardUiContext = FRAGMENT_GALLERY,
    templateProvider = {
        sampleFragmentSetupTemplate
    },
    dialogTitle = "android.wizard.new.fragment.title",
    stepTitle = "android.wizard.config.fragment.title"
)