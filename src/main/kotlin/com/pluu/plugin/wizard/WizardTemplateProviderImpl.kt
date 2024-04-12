package com.pluu.plugin.wizard

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:wizard/template-impl/src/com/android/tools/idea/wizard/template/impl/WizardTemplateProviderImpl.kt
///////////////////////////////////////////////////////////////////////////

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