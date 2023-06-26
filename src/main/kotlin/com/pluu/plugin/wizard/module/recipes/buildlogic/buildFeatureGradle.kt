package com.pluu.plugin.wizard.module.recipes.buildlogic

import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock

internal fun buildFeatureGradle(
    applicationId: String
): String {
    val androidConfigBlock = androidFeatureConfig(
        applicationId = applicationId
    )

    val dependenciesBlock = """
  dependencies {  
  }
  """

    val allBlocks =
        """
    ${emptyPluginsBlock()}
    $androidConfigBlock
    $dependenciesBlock
    """

    return allBlocks
}

internal fun androidFeatureConfig(
    applicationId: String
): String {
    return """
    android {
    namespace '$applicationId'
    }
    """
}