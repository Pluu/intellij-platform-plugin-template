package com.pluu.plugin.wizard.module.recipes.buildlogic

import com.android.SdkConstants.FN_BUILD_GRADLE
import com.android.tools.idea.npw.module.recipes.addKotlinIfNeeded
import com.android.tools.idea.npw.module.recipes.benchmarkModule.buildGradle
import com.android.tools.idea.npw.module.recipes.benchmarkModule.src.main.androidManifestXml
import com.android.tools.idea.npw.module.recipes.gitignore
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.npw.module.recipes.benchmarkModule.src.androidTest.androidManifestXml as testAndroidManifestXml

fun RecipeExecutor.generateFeatureModule(
    moduleData: ModuleTemplateData
) {
    val projectData = moduleData.projectTemplateData
    val testOut = moduleData.testDir
    val packageName = moduleData.packageName
    val moduleOut = moduleData.rootDir
    val (buildApi, targetApi, minApi) = moduleData.apis
    val language = projectData.language

    addIncludeToSettings(moduleData.name)
    val bg = buildGradle(
        packageName,
        buildApi.apiString,
        minApi.apiString,
        targetApi.apiString,
        language,
        projectData.gradlePluginVersion,
        false
    )

    save(bg, moduleOut.resolve(FN_BUILD_GRADLE))
    applyPlugin("com.android.library", projectData.gradlePluginVersion)
    addDependency("androidx.test:runner:+", "androidTestImplementation")
    addDependency("androidx.test.ext:junit:+", "androidTestImplementation")
    addDependency("junit:junit:4.+", "androidTestImplementation", "4.13.2")

    save(androidManifestXml(), moduleOut.resolve("src/main/AndroidManifest.xml"))
    save(testAndroidManifestXml(), moduleOut.resolve("src/androidTest/AndroidManifest.xml"))
    save(gitignore(), moduleOut.resolve(".gitignore"))

    addKotlinIfNeeded(projectData, targetApi = targetApi.api, noKtx = true)
}