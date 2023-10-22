package com.pluu.plugin.wizard.module.recipes.feature.sample

import com.android.ide.common.repository.AgpVersion
import com.android.tools.idea.npw.module.recipes.androidConfig
import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock
import com.android.tools.idea.wizard.template.BaseFeature
import com.android.tools.idea.wizard.template.CppStandardType
import com.pluu.plugin.wizard.module.recipes.feature.androidFeatureConfig

internal fun buildFeatureSampleGradle(
    isKts: Boolean,
    isLibraryProject: Boolean,
    applicationId: String,
    useVersionCatalog: Boolean,
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
    ${emptyPluginsBlock(isKts = isKts, useVersionCatalog = useVersionCatalog)}
    $androidConfigBlock
    $dependenciesBlock
"""
}

internal fun buildFeatureSampleDefaultGradle(
    agpVersion: AgpVersion,
    isKts: Boolean,
    /** The application ID; also used for the namespace. */
    applicationId: String,
    buildApiString: String,
    minApi: String,
    targetApi: String,
    useAndroidX: Boolean,
    baseFeature: BaseFeature,
    hasTests: Boolean = true,
    addLintOptions: Boolean = false,
    useVersionCatalog: Boolean
): String {
    val androidConfigBlock = androidConfig(
        agpVersion = agpVersion,
        buildApiString = buildApiString,
        minApi = minApi,
        targetApi = targetApi,
        useAndroidX = useAndroidX,
        isLibraryProject = false,
        isDynamicFeature = false,
        explicitApplicationId = true,
        applicationId = applicationId,
        hasTests = hasTests,
        canUseProguard = true,
        addLintOptions = addLintOptions,
        enableCpp = false,
        cppStandard = CppStandardType.`Toolchain Default`
    )

    val dependenciesBlock =
        """
      dependencies {
        implementation project("${baseFeature.name}")
      }
    """

    return """
${emptyPluginsBlock(isKts = isKts, useVersionCatalog = useVersionCatalog)}
$androidConfigBlock
$dependenciesBlock
"""
}