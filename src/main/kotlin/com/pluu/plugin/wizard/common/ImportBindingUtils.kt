package com.pluu.plugin.wizard.common

import com.android.tools.idea.wizard.template.renderIf

fun importDataBinding(
    isDataBindingSupported: Boolean
) = renderIf(isDataBindingSupported) {
    "import androidx.databinding.DataBindingUtil"
}