package com.pluu.plugin.wizard.module.recipes.feature.sample

import com.android.SdkConstants
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleColors
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleStrings
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleThemes
import com.android.tools.idea.npw.module.recipes.copyMipmapFolder
import com.android.tools.idea.npw.module.recipes.generateManifest
import com.android.tools.idea.npw.module.recipes.gitignore
import com.android.tools.idea.npw.module.recipes.proguardRecipe
import com.android.tools.idea.wizard.template.Category
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.wizard.template.impl.activities.common.addMaterialDependency
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.module.recipes.androidModule.gradleToKtsOrDcl

fun RecipeExecutor.generateFeatureSampleModule(
    moduleData: ModuleTemplateData,
    appTitle: String?, // may be null only for libraries
    appTitleResName: String = "app_name",
) {
    val (projectData, srcOut, resOut, manifestOut, _, _, _, moduleOut) = moduleData
    val (_, agpVersion) = projectData
    val appCompatVersion = moduleData.apis.appCompatVersion
    val isLibraryProject = moduleData.isLibrary
    val baseFeature = moduleData.baseFeature!!
    val namespace = moduleData.namespace
    val isKts = true

    createDirectory(srcOut)
    addIncludeToSettings(moduleData.name)

    val gradleFile = buildFeatureSampleGradle(
        isLibraryProject = isLibraryProject,
        applicationId = namespace,
        baseFeature = baseFeature
    ).gradleToKtsOrDcl(apply = isKts)

    val buildFile = if (isKts) SdkConstants.FN_BUILD_GRADLE_KTS else SdkConstants.FN_BUILD_GRADLE

    save(
        gradleFile,
        moduleOut.resolve(buildFile)
    )

    // build-logic
    applyPlugin(PluuPlugin.Convension.APPLICATION, null)
    applyPlugin(PluuPlugin.Convension.HILT, null)
    val version = projectData.agpVersion.toString()
//    applyPlugin(PluuPlugin.Convension.KAPT, version)

    addMaterialDependency(true)
    addDependency("com.android.support:appcompat-v7:$appCompatVersion.+")
    addDependency("com.android.support.constraint:constraint-layout:+")

    // Add, Manifest
    val manifestXml = generateManifest(
        hasApplicationBlock = !isLibraryProject,
        theme = "@style/${moduleData.themesData.main.name}",
        addBackupRules = false
    )
    save(manifestXml, manifestOut.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML))
    save(gitignore(), moduleOut.resolve(".gitignore"))
    proguardRecipe(moduleOut, agpVersion, isLibraryProject)

    val themesXml = androidModuleThemes(true, moduleData.apis.minApi, moduleData.themesData.main.name)
    val colorsXml = androidModuleColors()

    if (!isLibraryProject) {
        // Icon
        copyMipmapFolder(resOut)

        with(resOut.resolve(SdkConstants.FD_RES_VALUES)) {
            save(androidModuleStrings(appTitleResName, appTitle!!), resolve("strings.xml"))
            // Common themes.xml isn't needed for Compose because theme is created in Composable.
            if (moduleData.category != Category.Compose) {
                save(themesXml, resolve("themes.xml"))
            }
            save(colorsXml, resolve("colors.xml"))
        }
    }
}