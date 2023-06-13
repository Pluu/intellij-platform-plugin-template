package com.pluu.wizard.activity

import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.wizard.template.impl.activities.common.addAllKotlinDependencies
import com.android.tools.idea.wizard.template.impl.activities.common.addLifecycleDependencies
import com.android.tools.idea.wizard.template.impl.activities.common.addMaterialDependency
import com.android.tools.idea.wizard.template.impl.activities.common.generateManifest
import com.android.tools.idea.wizard.template.impl.activities.common.generateSimpleLayout
import com.pluu.wizard.activity.src.basicActivityKt
import com.pluu.wizard.common.ViewBindingType
import com.pluu.wizard.common.generateDataBindingSimpleXml
import com.pluu.wizard.common.viewmodel.viewModelKt

fun RecipeExecutor.sampleActivitySetup(
    moduleData: ModuleTemplateData,
    packageName: String,
    activityClass: String,
    layoutName: String,
    isUsedViewModel: Boolean,
    viewModelClass: String,
    viewBindingType: ViewBindingType,
) {
    val (projectData, srcOut, resOut) = moduleData
    val appCompatVersion = moduleData.apis.appCompatVersion
    val useAndroidX = moduleData.projectTemplateData.androidXSupport
    val ktOrJavaExt = projectData.language.extension
    val generateKotlin = projectData.language == Language.Kotlin

    // Add, Dependencies
    addAllKotlinDependencies(moduleData)
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
        generateActivityTitle = true
    )

    // Create, Activity
    val simpleActivityPath = srcOut.resolve("$activityClass.$ktOrJavaExt")
    val simpleActivity = basicActivityKt(
        isNewProject = moduleData.isNewModule,
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
        generateSimpleLayout(moduleData, activityClass, layoutName)
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

    if (generateKotlin) {
        requireJavaVersion("1.8", true)
    }
}