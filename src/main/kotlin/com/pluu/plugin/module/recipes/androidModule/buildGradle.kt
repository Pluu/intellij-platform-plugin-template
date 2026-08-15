package com.pluu.plugin.module.recipes.androidModule

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android-npw/src/com/android/tools/idea/npw/module/recipes/androidModule/buildGradle.kt
///////////////////////////////////////////////////////////////////////////

private fun String.toKtsFunction(funcName: String): String =
    if (this.contains("$funcName ")) {
        this.replace("$funcName ", "$funcName(") + ")"
    } else {
        this
    }

private fun String.toKtsProperty(funcName: String): String = this.replace(Regex("$funcName\\s(?![={])"), "$funcName = ")

internal fun String.gradleToKtsOrDcl(apply: Boolean): String =
    if (apply) {
        split("\n").joinToString("\n") {
            it
                .replace("'", "\"")
                .toKtsProperty("namespace")
                .toKtsFunction("compileSdkVersion")
                .toKtsProperty("compileSdk")
                .toKtsProperty("compileSdkPreview")
                .toKtsProperty("buildToolsVersion")
                .toKtsProperty("applicationId")
                .toKtsFunction("minSdkVersion")
                .toKtsProperty("minSdk")
                .toKtsProperty("minSdkPreview")
                .toKtsFunction("targetSdkVersion")
                .toKtsProperty("targetSdk")
                .toKtsProperty("targetSdkPreview")
                .toKtsProperty("versionCode")
                .toKtsProperty("versionName")
                .toKtsProperty("testInstrumentationRunner")
                .toKtsProperty("minifyEnabled")
                .toKtsProperty("enable")
                .toKtsFunction("proguardFiles")
                .toKtsFunction("consumerProguardFiles")
                .toKtsFunction("implementation") // For dynamic app: implementation project(":app") -> implementation(project(":app"))
                .replace("minifyEnabled", "isMinifyEnabled")
                .replace("debuggable", "isDebuggable")
                // The followings are for externalNativeBuild
                .toKtsFunction("cppFlags")
                .toKtsFunction("path")
                .toKtsProperty("version")
                .toKtsProperty("enableKotlin")
        }
    } else {
        this
    }
