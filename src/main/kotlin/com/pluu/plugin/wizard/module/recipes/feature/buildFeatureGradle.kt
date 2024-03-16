package com.pluu.plugin.wizard.module.recipes.feature

import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock

internal fun buildFeatureGradle(
    isLibraryProject: Boolean,
    applicationId: String
): String {
    val androidConfigBlock = androidFeatureConfig(
        isLibraryProject = isLibraryProject,
        applicationId = applicationId
    )

    val dependenciesBlock = """
      dependencies {  
      }
    """

    return """
    ${emptyPluginsBlock()}
    $androidConfigBlock
    $dependenciesBlock
"""
}