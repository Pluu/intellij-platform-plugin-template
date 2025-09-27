package com.pluu.plugin.action

import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.FRAGMENT_GALLERY
import com.pluu.plugin.PluuBundle
import com.pluu.plugin.action.component.UiComponentCreateWizardAction

class FragmentCreateWizardAction : UiComponentCreateWizardAction(
    wizardUiContext = FRAGMENT_GALLERY,
    templateName = PluuBundle.message("pluu.fragment.and.viewmodel.new.feature.title"),
    dialogTitle = "android.wizard.new.fragment.title",
    stepTitle = "android.wizard.config.fragment.title"
)