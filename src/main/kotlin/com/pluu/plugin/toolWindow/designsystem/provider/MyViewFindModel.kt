package com.pluu.plugin.toolWindow.designsystem.provider

import com.android.annotations.concurrency.WorkerThread
import com.android.ide.common.resources.ResourceItem
import com.android.ide.common.resources.configuration.FolderConfiguration
import com.android.resources.ResourceType
import com.android.resources.ResourceVisibility
import com.android.resources.base.RepositoryConfiguration
import com.android.tools.idea.res.ResourceFolderRegistry
import com.android.tools.idea.res.VfsFileResourceItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.EmptyQuery
import com.intellij.util.Query
import org.jetbrains.android.dom.converters.PackageClassConverter
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.ResourceFolderManager
import java.util.function.Function

class MyViewFindModel {
    private val VIEW_CLASSES_QUERY = Function<Project, Query<PsiClass>> { project ->
        val viewClass = JavaPsiFacade.getInstance(project)
            .findClass("android.view.View", GlobalSearchScope.allScope(project))
        if (viewClass == null) {
            // There is probably no SDK
            return@Function EmptyQuery.getEmptyQuery()
        }
        ClassInheritorsSearch.search(viewClass, ProjectScope.getProjectScope(project), true)
    }

    @WorkerThread
    fun setProject(forFacet: AndroidFacet): List<ResourceItem> {
        val project = forFacet.module.project
        return CustomViewInfo.fromPsiClasses(VIEW_CLASSES_QUERY.apply(project))
            .map {
                val repository = ResourceFolderRegistry.getInstance(project).get(
                    forFacet,
                    ResourceFolderManager.getInstance(forFacet).folders[0]
                )

                VfsFileResourceItem(
                    ResourceType.LAYOUT,
                    "layout_my_view",
                    RepositoryConfiguration(
                        repository,
                        FolderConfiguration.createDefault()
                    ),
                    ResourceVisibility.UNDEFINED,
                    "./layout/layout_my_view.xml",
                    repository.resourceDir.findFileByRelativePath("layout/layout_my_view.xml")
                )
            }
    }

    private class CustomViewInfo(
        val description: String,
        val tagName: String,
        val className: String,
        val file: VirtualFile,
        val xmlFile: VirtualFile
    ) {
        companion object {
            fun fromPsiClasses(psiClasses: Iterable<PsiClass>): List<CustomViewInfo> {
                val componentInfos = ArrayList<CustomViewInfo>()

                psiClasses.forEach { psiClass ->
                    val description = psiClass.name // We use the "simple" name as description on the preview.
                    val tagName = psiClass.qualifiedName
                    val className =
                        PackageClassConverter.getQualifiedName(psiClass)

                    if (description == null || tagName == null || className == null) {
                        // Currently we ignore anonymous views
                        return@forEach
                    }
                    val virtualFile = psiClass.containingFile.virtualFile
                    componentInfos.add(
                        CustomViewInfo(
                            description,
                            tagName,
                            className,
                            virtualFile,
                            virtualFile
                        )
                    )
                }

                return componentInfos
            }
        }
    }
}