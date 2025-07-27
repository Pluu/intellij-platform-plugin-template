package com.pluu.plugin.wizard.common.viewmodel

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
    isViewModelSupported: Boolean
) = renderIf(isViewModelSupported) {
    "import androidx.activity.viewModels"
}

fun importFragmentViewModel(
    isViewModelSupported: Boolean,
    isUsedSharedViewModel:Boolean
) = renderIf(isViewModelSupported) {
    if (!isUsedSharedViewModel) {
        "import androidx.fragment.app.viewModels"
    } else {
        "import androidx.fragment.app.activityViewModels"
    }
}