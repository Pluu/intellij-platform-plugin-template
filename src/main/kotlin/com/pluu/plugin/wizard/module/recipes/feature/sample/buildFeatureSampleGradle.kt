package com.pluu.plugin.wizard.module.recipes.feature.sample

import com.android.tools.idea.npw.module.recipes.androidConfig
import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock
import com.android.tools.idea.wizard.template.BaseFeature
import com.android.tools.idea.wizard.template.CppStandardType
import com.android.tools.idea.wizard.template.GradlePluginVersion

internal fun buildFeatureSampleGradle(
    isKts: Boolean,
    applicationId: String,
    useVersionCatalog: Boolean,
    baseFeature: BaseFeature
): String {
    val androidConfigBlock = androidFeatureConfig(
        applicationId = applicationId
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

internal fun androidFeatureConfig(
    applicationId: String
): String {
    return """
    android {
    namespace '$applicationId'
    }
    """
}

internal fun buildFeatureSampleDefaultGradle(
    agpVersion: GradlePluginVersion,
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
        gradlePluginVersion = agpVersion,
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