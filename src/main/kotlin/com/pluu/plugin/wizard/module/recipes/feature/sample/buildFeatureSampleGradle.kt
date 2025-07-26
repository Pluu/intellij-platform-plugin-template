package com.pluu.plugin.wizard.module.recipes.feature.sample

import com.android.ide.common.repository.AgpVersion
import com.android.sdklib.AndroidMajorVersion
import com.android.sdklib.AndroidVersion
import com.android.tools.idea.npw.module.recipes.androidConfig
import com.android.tools.idea.npw.module.recipes.emptyPluginsBlock
import com.android.tools.idea.wizard.template.BaseFeature
import com.android.tools.idea.wizard.template.CppStandardType
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

internal fun buildFeatureSampleDefaultGradle(
    agpVersion: AgpVersion,
    /** The application ID; also used for the namespace. */
    applicationId: String,
    buildApi: AndroidVersion,
    minApi: AndroidMajorVersion,
    targetApi: AndroidMajorVersion,
    useAndroidX: Boolean,
    baseFeature: BaseFeature,
    hasTests: Boolean = true,
    addLintOptions: Boolean = false
): String {
    val androidConfigBlock = androidConfig(
        agpVersion = agpVersion,
        buildApi = buildApi,
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
${emptyPluginsBlock()}
$androidConfigBlock
$dependenciesBlock
"""
}