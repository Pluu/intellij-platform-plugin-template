package com.pluu.wizard

import com.android.tools.idea.wizard.template.Template
import com.android.tools.idea.wizard.template.WizardTemplateProvider
import com.pluu.wizard.activity.sampleActivitySetupTemplate
import com.pluu.wizard.fragment.sampleFragmentSetupTemplate

class WizardTemplateProviderImpl: WizardTemplateProvider() {
    override fun getTemplates(): List<Template> = listOf(
        sampleActivitySetupTemplate,
        sampleFragmentSetupTemplate
    )
}