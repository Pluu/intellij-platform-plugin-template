package com.pluu.plugin.wizard.common.viewmodel

import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.escapeKotlinIdentifier
import com.android.tools.idea.wizard.template.getMaterialComponentName
import com.android.tools.idea.wizard.template.renderIf

fun viewModelKt(
    packageName: String,
    useAndroidX: Boolean,
    viewModelClass: String
) = """
package ${escapeKotlinIdentifier(packageName)}

import ${getMaterialComponentName("android.arch.lifecycle.ViewModel", useAndroidX)}

class $viewModelClass : ViewModel() {
    // TODO: Implement the ViewModel
}
"""

fun importActivityViewModel(
    isViewModelSupported: Boolean,
    language: Language
) = renderIf(isViewModelSupported) {
    "import androidx.activity.viewModels${renderIf(language == Language.Java) { ";" }}"
}

fun importFragmentViewModel(
    isViewModelSupported: Boolean,
    isUsedSharedViewModel:Boolean,
    language: Language
) = renderIf(isViewModelSupported) {
    if (!isUsedSharedViewModel) {
        "import androidx.fragment.app.viewModels${renderIf(language == Language.Java) { ";" }}"
    } else {
        "import androidx.fragment.app.activityViewModels${renderIf(language == Language.Java) { ";" }}"
    }
}