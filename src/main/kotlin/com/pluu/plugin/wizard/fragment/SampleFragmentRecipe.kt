package com.pluu.plugin.wizard.fragment

import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.wizard.template.impl.activities.common.addAllKotlinDependencies
import com.android.tools.idea.wizard.template.impl.activities.common.addLifecycleDependencies
import com.android.tools.idea.wizard.template.impl.activities.common.addMaterialDependency
import com.android.tools.idea.wizard.template.impl.fragments.blankFragment.res.layout.fragmentBlankXml
import com.pluu.plugin.utils.ModuleUtils
import com.pluu.plugin.wizard.common.ViewBindingType
import com.pluu.plugin.wizard.common.generateDataBindingSimpleXml
import com.pluu.plugin.wizard.common.viewmodel.viewModelKt
import com.pluu.plugin.wizard.fragment.src.basicFragmentKt

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:wizard/template-impl/src/com/android/tools/idea/wizard/template/impl/fragments/viewModelFragment/viewModelFragmentRecipe.kt
///////////////////////////////////////////////////////////////////////////

fun RecipeExecutor.sampleFragmentSetup(
    moduleData: ModuleTemplateData,
    packageName: String,
    fragmentClass: String,
    layoutName: String,
    isUsedViewModel: Boolean,
    isUsedSharedViewModel: Boolean,
    viewModelClass: String,
    viewBindingType: ViewBindingType,
) {
    val (projectData, srcOut, resOut) = moduleData
    val useAndroidX = moduleData.projectTemplateData.androidXSupport
    val ktOrJavaExt = projectData.language.extension
    val generateKotlin = projectData.language == Language.Kotlin
    val useConventionPlugin = ModuleUtils.useConventionPlugin(moduleData.rootDir)

    // Add, Dependencies
    if (!useConventionPlugin) {
        addAllKotlinDependencies(moduleData)
    }
    addDependency("com.android.support.constraint:constraint-layout:+")
    if (isUsedViewModel) {
        addDependency("androidx.fragment:fragment-ktx:+")
        addLifecycleDependencies(true)
    }
    addMaterialDependency(useAndroidX)

    // Create, Fragment
    val simpleFragmentPath = srcOut.resolve("$fragmentClass.$ktOrJavaExt")
    val simpleFragment = basicFragmentKt(
        applicationPackage = projectData.applicationPackage,
        packageName = packageName,
        useAndroidX = useAndroidX,
        fragmentClass = fragmentClass,
        layoutName = layoutName,
        viewBindingType = viewBindingType,
        isUsedViewModel = isUsedViewModel,
        isUsedSharedViewModel = isUsedSharedViewModel,
        viewModelClass = viewModelClass,
    )
    save(simpleFragment, simpleFragmentPath)
    open(simpleFragmentPath)

    // Create, Layout
    val fragmentLayout = if (viewBindingType.isDataBinding) {
        generateDataBindingSimpleXml(fragmentClass, packageName)
    } else {
        fragmentBlankXml(fragmentClass, packageName)
    }
    save(fragmentLayout, resOut.resolve("layout/${layoutName}.xml"))
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
        requireJavaVersion("1.8", true)
    }
}