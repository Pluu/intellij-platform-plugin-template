package com.pluu.plugin.utils

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import org.jetbrains.android.facet.AndroidFacet

object ModuleUtils {
    fun isRootPlace(dataContext: DataContext): Boolean {
        val project = dataContext.getData(CommonDataKeys.PROJECT)
        val module = dataContext.getData(LangDataKeys.MODULE_CONTEXT)
        return project != null && module != null
    }

    fun isAndroidModulePlace(dataContext: DataContext): Boolean {
        val module = dataContext.getData(PlatformCoreDataKeys.MODULE)
        return module != null && AndroidFacet.getInstance(module) != null
    }
}