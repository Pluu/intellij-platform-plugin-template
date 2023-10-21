package com.pluu.plugin.wizard

import com.android.tools.idea.wizard.template.Template
import com.android.tools.idea.wizard.template.WizardTemplateProvider
import com.pluu.plugin.wizard.activity.sampleActivitySetupTemplate
import com.pluu.plugin.wizard.fragment.sampleFragmentSetupTemplate

class WizardTemplateProviderImpl: WizardTemplateProvider() {
    override fun getTemplates(): List<Template> = listOf(
        sampleActivitySetupTemplate,
        sampleFragmentSetupTemplate
    )
}