package com.pluu.plugin.toolWindow.designsystem.model

import com.android.SdkConstants
import com.android.annotations.concurrency.Slow
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.resources.ResourceItem
import com.android.ide.common.resources.ResourceRepository
import com.android.resources.ResourceType
import com.android.tools.idea.res.StudioResourceRepositoryManager
import com.android.tools.idea.ui.resourcemanager.model.ResourceDataManager
import com.android.tools.idea.ui.resourcemanager.model.ResourceSection
import com.android.tools.idea.ui.resourcemanager.model.TypeFilter
import com.android.tools.idea.ui.resourcemanager.model.TypeFilterKind
import com.android.tools.idea.ui.resourcemanager.model.createResourceSection
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiBinaryFile
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import org.jetbrains.android.facet.AndroidFacet

/**
 * Returns the list of local resources of the [forFacet] module.
 */
@Slow
fun getModuleResources(forFacet: AndroidFacet, type: DesignSystemType): ResourceSection {
    val moduleRepository = StudioResourceRepositoryManager.getModuleResources(forFacet)
    val sortedResources = moduleRepository.namespaces.flatMap { namespace ->
        moduleRepository.getResourcesAndApplyFilters(
            namespace,
            ResourceType.COLOR,
            emptyList<TypeFilter>(),
            forFacet
        )
    }.sortedBy { it.name }

    return createResourceSection(forFacet.module.name, sortedResources)
}

private fun ResourceRepository.getResourcesAndApplyFilters(
    namespace: ResourceNamespace,
    type: ResourceType,
    typeFilters: List<TypeFilter>,
    facet: AndroidFacet
): Collection<ResourceItem> {
    return if (typeFilters.isEmpty()) {
        getResources(namespace, type).values()
    } else {
        getResources(namespace, type) { resourceItem -> resourceItem.isValidForFilters(typeFilters, facet) }
    }
}

private fun ResourceItem.isValidForFilters(typeFilters: List<TypeFilter>, facet: AndroidFacet): Boolean {
    val dataManager =
        ResourceDataManager(facet) // TODO(148630535): This should not depend on ResourceDataManager, should be refactored out.
    val psiElement = runReadAction { dataManager.findPsiElement(this) } ?: return false
    when (psiElement) {
        is XmlFile -> {
            val tag = runReadAction { psiElement.rootTag } ?: return false
            // To verify XML Tag filters, we just compare the XML root tag value to the filter value, but unless we are intentionally filtering
            // for data-binding layouts, we then take the first non-data XML tag.
            typeFilters.forEach { filter ->
                if (filter.kind == TypeFilterKind.XML_TAG) {
                    if (tag.name == filter.value) {
                        return true
                    } else if (tag.name == SdkConstants.TAG_LAYOUT) {
                        // Is data-binding, look for the non-data tag.
                        val dataBindingViewTag =
                            tag.childrenOfType<XmlTag>().firstOrNull { it.name != SdkConstants.TAG_DATA }
                                ?: return false
                        if (dataBindingViewTag.name == filter.value) {
                            return true
                        }
                    }
                }
                if (filter.kind == TypeFilterKind.XML_TAG && tag.name == filter.value) {
                    return true
                }
            }
            return false
        }

        is PsiBinaryFile -> {
            val name = psiElement.name
            // To verify File filters, we look for the file extension, but we take the extension as the string after the first '.', since the
            // VirtualFile#getExtension method does not consider '.9.png' as an extension (returns 'png').
            typeFilters.forEach { filter ->
                val isFileFilter = filter.kind == TypeFilterKind.FILE
                val extension = name.substring(name.indexOf('.'))
                if (isFileFilter && extension.equals(filter.value, true)) {
                    return true
                }
            }
            return false
        }

        else -> return false
    }
}
