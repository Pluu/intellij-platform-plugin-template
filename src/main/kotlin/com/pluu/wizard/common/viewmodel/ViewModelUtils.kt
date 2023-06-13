package com.pluu.wizard.common.viewmodel

import com.android.tools.idea.wizard.template.classToResource
import com.android.tools.idea.wizard.template.underscoreToCamelCase

fun viewToViewModel(
    viewName: String
) = "${underscoreToCamelCase(classToResource(viewName))}ViewModel"