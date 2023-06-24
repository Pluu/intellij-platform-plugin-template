package com.pluu.plugin.wizard.module.recipes.buildlogic

import com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import com.android.SdkConstants.FN_BUILD_GRADLE
import com.android.tools.idea.npw.module.recipes.addInstrumentedTests
import com.android.tools.idea.npw.module.recipes.addKotlinIfNeeded
import com.android.tools.idea.npw.module.recipes.addLocalTests
import com.android.tools.idea.npw.module.recipes.addTestDependencies
import com.android.tools.idea.npw.module.recipes.androidModule.buildGradle
import com.android.tools.idea.npw.module.recipes.createDefaultDirectories
import com.android.tools.idea.npw.module.recipes.generateManifest
import com.android.tools.idea.npw.module.recipes.gitignore
import com.android.tools.idea.npw.module.recipes.proguardRecipe
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor

fun RecipeExecutor.generateFeatureModule(
    data: ModuleTemplateData,
    useKts: Boolean = false,
    bytecodeLevel: BytecodeLevel,
    addLintOptions: Boolean = false,
    useConventionPlugins: Boolean = false
) {
    val (projectData, srcOut, _, manifestOut, instrumentedTestOut, localTestOut, _, moduleOut) = data
    val (useAndroidX, agpVersion) = projectData
    val language = projectData.language
    val isLibraryProject = data.isLibrary
    val packageName = data.packageName
    val apis = data.apis
    val minApi = apis.minApi

    createDefaultDirectories(moduleOut, srcOut)
    addIncludeToSettings(data.name)
    save(
        buildGradle(
            agpVersion,
            isKts = useKts,
            isLibraryProject = isLibraryProject,
            isDynamicFeature = data.isDynamic,
            applicationId = data.namespace,
            buildApiString = apis.buildApi.apiString,
            minApi = minApi.apiString,
            targetApi = apis.targetApi.apiString,
            useAndroidX = useAndroidX,
            formFactorNames = projectData.includedFormFactorNames,
            hasTests = data.useGenericLocalTests,
            addLintOptions = addLintOptions
        ),
        moduleOut.resolve(FN_BUILD_GRADLE)
    )

    if (isLibraryProject) {
        if (useConventionPlugins) {
            // build-logic
            applyPlugin("pluu.android.library", null)
            applyPlugin("pluu.android.hilt", null)
        } else {
            applyPlugin("com.android.library", null)
        }
    }
    if (!useConventionPlugins) {
        addKotlinIfNeeded(projectData, targetApi = apis.targetApi.api, noKtx = true)
        requireJavaVersion(bytecodeLevel.versionString, data.projectTemplateData.language == Language.Kotlin)
    }

    if (data.useGenericLocalTests) {
        addLocalTests(packageName, localTestOut, language)
        addTestDependencies()
    }
    if (data.useGenericInstrumentedTests) {
        addInstrumentedTests(packageName, useAndroidX, isLibraryProject, instrumentedTestOut, language)
        addTestDependencies()
    }

    save(
        generateManifest(
            hasApplicationBlock = !isLibraryProject,
            theme = "@style/${data.themesData.main.name}",
        ),
        manifestOut.resolve(FN_ANDROID_MANIFEST_XML)
    )
    save(gitignore(), moduleOut.resolve(".gitignore"))

    proguardRecipe(moduleOut, isLibraryProject)
}