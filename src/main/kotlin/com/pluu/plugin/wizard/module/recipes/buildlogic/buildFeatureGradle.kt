package com.pluu.plugin.wizard.module.recipes.buildlogic

import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock

internal fun buildFeatureGradle(
    isKts: Boolean,
    applicationId: String,
    useVersionCatalog: Boolean
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
    ${emptyPluginsBlock(isKts = isKts, useVersionCatalog = useVersionCatalog)}
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