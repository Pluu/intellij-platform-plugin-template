package com.pluu.plugin.wizard.module.recipes.feature

import com.android.tools.idea.wizard.template.renderIf

internal fun androidFeatureConfig(
    isLibraryProject: Boolean,
    applicationId: String
): String {
    val propertiesBlock = renderIf(!isLibraryProject) {"""
    defaultConfig {
        applicationId "$applicationId"
        ${"versionCode 1" }
        ${"versionName \"1.0\"" }
    }    
"""}

    return """
    android {
    namespace '$applicationId'
    $propertiesBlock
    }
    """
}