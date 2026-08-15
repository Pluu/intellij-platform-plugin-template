package com.pluu.plugin.wizard.module.recipes.feature

import com.android.SdkConstants
import com.android.tools.idea.npw.module.recipes.addInstrumentedTests
import com.android.tools.idea.npw.module.recipes.addLocalTests
import com.android.tools.idea.npw.module.recipes.addTestDependencies
import com.android.tools.idea.npw.module.recipes.generateManifest
import com.android.tools.idea.npw.module.recipes.gitignore
import com.android.tools.idea.npw.module.recipes.proguardRecipe
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.module.recipes.androidModule.gradleToKtsOrDcl

///////////////////////////////////////////////////////////////////////////
// Origin
// - https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android-npw/src/com/android/tools/idea/npw/module/recipes/commonModuleRecipe.kt
///////////////////////////////////////////////////////////////////////////

fun RecipeExecutor.generateFeatureModule(
    data: ModuleTemplateData,
) {
    val (projectData, srcOut, _, manifestOut, instrumentedTestOut, localTestOut, _, moduleOut) = data
    val (useAndroidX, agpVersion) = projectData
    val language = projectData.language
    val isLibraryProject = data.isLibrary
    val packageName = data.packageName
    val isKts = true

    createDirectory(srcOut)
    addIncludeToSettings(data.name)

    val gradleFile = buildFeatureGradle(
        isLibraryProject = isLibraryProject,
        applicationId = data.namespace
    ).gradleToKtsOrDcl(apply = isKts)

    val buildFile = if (isKts) SdkConstants.FN_BUILD_GRADLE_KTS else SdkConstants.FN_BUILD_GRADLE

    save(
        gradleFile,
        moduleOut.resolve(buildFile)
    )

    if (isLibraryProject) {
        // build-logic
        applyPlugin(PluuPlugin.Convension.LIBRARY, null)
        applyPlugin(PluuPlugin.Convension.HILT, null)

//        val version = projectData.agpVersion.toString()
//        applyPlugin(PluuPlugin.Convension.KAPT, version)
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
        manifestOut.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML)
    )
    save(gitignore(), moduleOut.resolve(".gitignore"))

    proguardRecipe(moduleOut, agpVersion, isLibraryProject)
}