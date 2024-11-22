package com.pluu.plugin.wizard.activity

import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.wizard.template.impl.activities.common.addAllKotlinDependencies
import com.android.tools.idea.wizard.template.impl.activities.common.addLifecycleDependencies
import com.android.tools.idea.wizard.template.impl.activities.common.addMaterialDependency
import com.android.tools.idea.wizard.template.impl.activities.common.generateManifest
import com.android.tools.idea.wizard.template.impl.activities.common.generateSimpleLayout
import com.pluu.plugin.utils.ModuleUtils
import com.pluu.plugin.wizard.activity.src.basicActivityKt
import com.pluu.plugin.wizard.common.ViewBindingType
import com.pluu.plugin.wizard.common.generateDataBindingSimpleXml
import com.pluu.plugin.wizard.common.viewmodel.viewModelKt

///////////////////////////////////////////////////////////////////////////
// Origin
// - https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:wizard/template-impl/src/com/android/tools/idea/wizard/template/impl/activities/emptyActivity/emptyActivityRecipe.kt
// - https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:wizard/template-impl/src/com/android/tools/idea/wizard/template/impl/activities/common/commonRecipes.kt
///////////////////////////////////////////////////////////////////////////

fun RecipeExecutor.sampleActivitySetup(
    moduleData: ModuleTemplateData,
    packageName: String,
    activityClass: String,
    layoutName: String,
    containerId: String?,
    isUsedViewModel: Boolean,
    viewModelClass: String,
    viewBindingType: ViewBindingType,
) {
    val (projectData, srcOut, resOut) = moduleData
    val appCompatVersion = moduleData.apis.appCompatVersion
    val useAndroidX = moduleData.projectTemplateData.androidXSupport
    val ktOrJavaExt = projectData.language.extension
    val generateKotlin = projectData.language == Language.Kotlin
    val useConventionPlugin = ModuleUtils.useConventionPlugin(moduleData.rootDir)

    // Add, Dependencies
    if (!useConventionPlugin) {
        addAllKotlinDependencies(moduleData)
    }
    addMaterialDependency(useAndroidX)
    addDependency("com.android.support:appcompat-v7:$appCompatVersion.+")
    addDependency("com.android.support.constraint:constraint-layout:+")
    if (isUsedViewModel) {
        addDependency("androidx.activity:activity-ktx:+")
        addLifecycleDependencies(true)
    }

    // Add, Manifest
    generateManifest(
        moduleData, activityClass, packageName,
        isLauncher = false,
        hasNoActionBar = false,
        generateActivityTitle = false
    )

    // Create, Activity
    val simpleActivityPath = srcOut.resolve("$activityClass.$ktOrJavaExt")
    val simpleActivity = basicActivityKt(
        applicationPackage = projectData.applicationPackage,
        packageName = packageName,
        useAndroidX = useAndroidX,
        activityClass = activityClass,
        layoutName = layoutName,
        viewBindingType = viewBindingType,
        isUsedViewModel = isUsedViewModel,
        viewModelClass = viewModelClass,
    )
    save(simpleActivity, simpleActivityPath)
    open(simpleActivityPath)

    // Create, Layout
    if (viewBindingType.isDataBinding) {
        save(
            generateDataBindingSimpleXml(activityClass, packageName),
            resOut.resolve("layout/${layoutName}.xml")
        )
    } else {
        generateSimpleLayout(moduleData, activityClass, layoutName, containerId)
    }
    open(resOut.resolve("layout/${layoutName}.xml"))

    // Create, ViewModel
    if (isUsedViewModel) {
        val viewModel = viewModelKt(packageName, useAndroidX, viewModelClass)
        val viewModelPath = srcOut.resolve("${viewModelClass}.${ktOrJavaExt}")
        save(viewModel, viewModelPath)
        open(viewModelPath)
    }

    // Enable, BuildFeature
    if (viewBindingType.isViewBinding) {
        setBuildFeature("viewBinding", true)
    } else if (viewBindingType.isDataBinding) {
        setBuildFeature("dataBinding", true)
    }

    if (!useConventionPlugin && generateKotlin) {
        setJavaKotlinCompileOptions(true)
    }
}