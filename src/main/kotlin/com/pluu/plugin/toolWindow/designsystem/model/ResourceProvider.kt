package com.pluu.plugin.toolWindow.designsystem.model

import com.android.annotations.concurrency.Slow
import com.android.tools.idea.ui.resourcemanager.model.ResourceSection
import com.android.tools.idea.ui.resourcemanager.model.createResourceSection
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import org.jetbrains.android.facet.AndroidFacet

/**
 * Returns the list of local resources of the [forFacet] module.
 */
@Slow
fun getModuleResources(forFacet: AndroidFacet, type: DesignSystemType): ResourceSection {
//    val moduleRepository = StudioResourceRepositoryManager.getModuleResources(forFacet)
//    val sortedResources = moduleRepository.namespaces.flatMap { namespace ->
//        moduleRepository.getResourcesAndApplyFilters(namespace, type, true, typeFilters, forFacet)
//    }.sortedBy { it.name }

    return createResourceSection(forFacet.module.name, emptyList())
}