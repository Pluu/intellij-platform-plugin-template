package com.pluu.plugin.wizard.module.recipes.feature.sample

import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock
import com.android.tools.idea.wizard.template.BaseFeature
import com.pluu.plugin.wizard.module.recipes.feature.androidFeatureConfig

internal fun buildFeatureSampleGradle(
    isLibraryProject: Boolean,
    applicationId: String,
    baseFeature: BaseFeature
): String {
    val androidConfigBlock = androidFeatureConfig(
        isLibraryProject = isLibraryProject,
        applicationId = applicationId
    )

    val dependenciesBlock = """
      dependencies {
        implementation project("${baseFeature.name}")
      }
    """

    return """
    ${emptyPluginsBlock()}
    $androidConfigBlock
    $dependenciesBlock
"""
}
