package com.pluu.plugin.toolWindow.designsystem.model

import com.android.annotations.concurrency.Slow
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.resources.ResourceItem
import com.android.ide.common.resources.ResourceRepository
import com.android.resources.ResourceType
import com.android.tools.idea.res.StudioResourceRepositoryManager
import com.android.tools.idea.ui.resourcemanager.model.ResourceSection
import com.android.tools.idea.ui.resourcemanager.model.createResourceSection
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import org.jetbrains.android.facet.AndroidFacet

/**
 * Returns the list of local resources of the [forFacet] module.
 */
@Slow
fun getModuleResources(
    forFacet: AndroidFacet,
    type: DesignSystemType
): ResourceSection {
    val moduleRepository = StudioResourceRepositoryManager.getModuleResources(forFacet)
    val sortedResources = moduleRepository.namespaces.flatMap { namespace ->
        moduleRepository.getResourcesAndApplyFilters(
            namespace,
            ResourceType.COLOR
        )
    }.sortedBy { it.name }

    return createResourceSection(forFacet.module.name, sortedResources)
}

private fun ResourceRepository.getResourcesAndApplyFilters(
    namespace: ResourceNamespace,
    type: ResourceType
): Collection<ResourceItem> {
    return getResources(namespace, type).values()
}