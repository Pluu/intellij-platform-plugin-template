package com.pluu.plugin.toolWindow.designsystem.model

import com.android.annotations.concurrency.Slow
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.resources.ResourceItem
import com.android.ide.common.resources.ResourceRepository
import com.android.resources.ResourceType
import com.android.tools.idea.res.AndroidDependenciesCache
import com.android.tools.idea.res.StudioResourceRepositoryManager
import com.android.tools.idea.ui.resourcemanager.model.ResourceSection
import com.android.tools.idea.ui.resourcemanager.model.TypeFilter
import com.android.tools.idea.ui.resourcemanager.model.createResourceSection
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
): ResourceSection {
    val moduleRepository = StudioResourceRepositoryManager.getModuleResources(forFacet)
    val sortedResources = moduleRepository.namespaces.flatMap { namespace ->
        moduleRepository.getResourcesAndApplyFilters(
            namespace,
            ResourceType.COLOR,
            forFacet
        )
    }.sortedBy { it.name }

    return createResourceSection(forFacet.module.name, sortedResources)
}

/**
 * Returns a list of resources from other modules that [forFacet] depends on.
 *
 * Eg:
 *
 * If **moduleA** depends on **moduleB**, this function returns the list of local resources of **moduleB**.
 */
@Slow
fun getDependentModuleResources(
    forFacet: AndroidFacet,
    type: DesignSystemType,
    typeFilters: List<TypeFilter>
): List<ResourceSection> {
    return AndroidDependenciesCache.getAndroidResourceDependencies(forFacet.module).asSequence()
        .flatMap { dependentFacet ->
            val moduleRepository = StudioResourceRepositoryManager.getModuleResources(dependentFacet)
            moduleRepository.namespaces.asSequence()
                .map { namespace ->
                    moduleRepository.getResourcesAndApplyFilters(
                        namespace, ResourceType.COLOR, forFacet
                    )
                }
                .filter { it.isNotEmpty() }
                .map { createResourceSection(dependentFacet.module.name, it.sortedBy(ResourceItem::getName)) }
        }.toList()
}

private fun ResourceRepository.getResourcesAndApplyFilters(
    namespace: ResourceNamespace,
    type: ResourceType,
    forFacet: AndroidFacet
): Collection<ResourceItem> {
    // TODO: 관련 리소스 가져오는 코드 추가
    val p = MyViewFindModel()
    return p.setProject(forFacet)
//    return getResources(namespace, type).values()
}