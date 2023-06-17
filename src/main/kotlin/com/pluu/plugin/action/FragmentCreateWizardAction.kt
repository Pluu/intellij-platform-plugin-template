package com.pluu.plugin.action

import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.FRAGMENT_GALLERY
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.action.component.UiComponentCreateWizardAction
import icons.StudioIcons

class FragmentCreateWizardAction : UiComponentCreateWizardAction(
    text = "Create Fragment",
    wizardUiContext = FRAGMENT_GALLERY,
    templateName = PluuPlugin.FRAGMENT_WITH_VIEWMODEL,
    dialogTitle = "android.wizard.new.fragment.title",
    stepTitle = "android.wizard.config.fragment.title"
) {
    init {
        templatePresentation.icon = StudioIcons.Shell.Filetree.ANDROID_FILE
    }
}