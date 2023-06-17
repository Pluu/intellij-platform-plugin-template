package com.pluu.plugin.wizard.common

import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.renderIf

fun importDataBinding(
    isDataBindingSupported: Boolean,
    language: Language
) = renderIf(isDataBindingSupported) {
    "import androidx.databinding.DataBindingUtil${renderIf(language == Language.Java) { ";" }}"
}