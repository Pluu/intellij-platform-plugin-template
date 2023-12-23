package com.pluu.plugin.toolWindow.designsystem.model

import com.android.annotations.concurrency.Slow
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.provider.MyViewFindModel
import org.jetbrains.android.facet.AndroidFacet

/**
 * Returns the list of local resources of the [forFacet] module.
 */
@Slow
fun getModuleResources(
    forFacet: AndroidFacet,
    type: DesignSystemType
): List<DesignSystemItem> {
    return MyViewFindModel.findDesignKit(forFacet, type)
}