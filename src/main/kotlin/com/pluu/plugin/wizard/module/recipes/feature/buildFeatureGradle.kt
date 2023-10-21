package com.pluu.plugin.wizard.module.recipes.feature

import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock

internal fun buildFeatureGradle(
    isKts: Boolean,
    isLibraryProject: Boolean,
    applicationId: String,
    useVersionCatalog: Boolean
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
    ${emptyPluginsBlock(isKts = isKts, useVersionCatalog = useVersionCatalog)}
    $androidConfigBlock
    $dependenciesBlock
"""
}