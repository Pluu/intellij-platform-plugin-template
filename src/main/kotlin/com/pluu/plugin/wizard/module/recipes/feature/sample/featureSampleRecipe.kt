package com.pluu.plugin.wizard.module.recipes.feature.sample

import com.android.SdkConstants
import com.android.tools.idea.npw.module.recipes.addKotlinIfNeeded
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleColors
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleStrings
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleThemes
import com.android.tools.idea.npw.module.recipes.copyMipmapFolder
import com.android.tools.idea.npw.module.recipes.createDefaultDirectories
import com.android.tools.idea.npw.module.recipes.generateManifest
import com.android.tools.idea.npw.module.recipes.gitignore
import com.android.tools.idea.npw.module.recipes.proguardRecipe
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.android.tools.idea.wizard.template.Category
import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor

fun RecipeExecutor.generateFeatureSampleModule(
    moduleData: ModuleTemplateData,
    appTitle: String?, // may be null only for libraries
    useGradleKts: Boolean = false,
    bytecodeLevel: BytecodeLevel = BytecodeLevel.default,
    useVersionCatalog: Boolean = true,
    useConventionPlugins: Boolean = false
) {
    val (projectData, srcOut, resOut, manifestOut, _, _, _, moduleOut) = moduleData
    val (useAndroidX, agpVersion) = projectData
    val language = projectData.language
    val isLibraryProject = moduleData.isLibrary
    val apis = moduleData.apis
    val minApi = apis.minApi
    val baseFeature = moduleData.baseFeature!!
    val namespace = moduleData.namespace

    createDefaultDirectories(moduleOut, srcOut)
    addIncludeToSettings(moduleData.name)

    val buildFile = if (useGradleKts) SdkConstants.FN_BUILD_GRADLE_KTS else SdkConstants.FN_BUILD_GRADLE

    val gradleFile: String = if (useConventionPlugins) {
        buildFeatureSampleGradle(
            isKts = useGradleKts,
            applicationId = namespace,
            useVersionCatalog = useVersionCatalog,
            baseFeature = baseFeature
        )
    } else {
        buildFeatureSampleDefaultGradle(
            agpVersion = agpVersion,
            isKts = useGradleKts,
            applicationId = namespace,
            buildApiString = apis.buildApi.apiString,
            minApi = minApi.apiString,
            targetApi = apis.targetApi.apiString,
            useAndroidX = useAndroidX,
            baseFeature = baseFeature,
            hasTests = false,
            addLintOptions = false,
            useVersionCatalog = useVersionCatalog
        )
    }
    save(
        gradleFile,
        moduleOut.resolve(buildFile)
    )

    if (useConventionPlugins) {
        // TODO: Custom convention plugin
        applyPlugin("com.android.application", agpVersion)
        addKotlinIfNeeded(projectData, targetApi = apis.targetApi.api, noKtx = true)
    } else {
        applyPlugin("com.android.application", agpVersion)
        addKotlinIfNeeded(projectData, targetApi = apis.targetApi.api, noKtx = true)
    }
    if (!useConventionPlugins) {
        addKotlinIfNeeded(projectData, targetApi = apis.targetApi.api, noKtx = true)
        requireJavaVersion(bytecodeLevel.versionString, language == Language.Kotlin)
    }

    val manifestXml = generateManifest(
        hasApplicationBlock = !isLibraryProject,
        theme = "@style/${moduleData.themesData.main.name}",
        addBackupRules = false
    )

    save(manifestXml, manifestOut.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML))
    save(gitignore(), moduleOut.resolve(".gitignore"))
    proguardRecipe(moduleOut, isLibraryProject)

    val themesXml = androidModuleThemes(useAndroidX, moduleData.apis.minApi, moduleData.themesData.main.name)
    val colorsXml = androidModuleColors()

    if (!isLibraryProject) {
        // Icon
        copyMipmapFolder(resOut)

        with(resOut.resolve(SdkConstants.FD_RES_VALUES)) {
            save(androidModuleStrings(appTitle!!), resolve("strings.xml"))
            // Common themes.xml isn't needed for Compose because theme is created in Composable.
            if (moduleData.category != Category.Compose) {
                save(themesXml, resolve("themes.xml"))
            }
            save(colorsXml, resolve("colors.xml"))
        }
    }
}