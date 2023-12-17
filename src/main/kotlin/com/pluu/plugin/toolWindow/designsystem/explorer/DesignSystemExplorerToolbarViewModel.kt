package com.pluu.plugin.toolWindow.designsystem.explorer

import com.android.tools.idea.projectsystem.SourceProviderManager
import com.intellij.ide.IdeView
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.findCompatibleFacets
import com.pluu.plugin.toolWindow.designsystem.getFacetForModuleName
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import org.jetbrains.android.facet.AndroidFacet
import kotlin.properties.Delegates

class DesignSystemExplorerToolbarViewModel(
    facet: AndroidFacet,
    initialDesignSystemType: DesignSystemType,
    private val filterOptions: FilterOptions
) : DataProvider, IdeView {

    /**
     * Callback added by the view to be called when data of this
     * view model changes.
     */
    var updateUICallback = {}

    /** Called when a new facet is selected. */
    var facetUpdaterCallback: (AndroidFacet) -> Unit = {}

    /** Callback for when a new resource is created from a toolbar action. */
    var resourceUpdaterCallback: ((String, DesignSystemType) -> Unit)? = null

    var resourceType: DesignSystemType by Delegates.observable(initialDesignSystemType) { _, oldValue, newValue ->
        if (newValue != oldValue) {
            updateUICallback()
        }
    }

    var facet: AndroidFacet = facet
        set(newFacet) {
            if (field != newFacet) {
                field = newFacet
                updateUICallback()
            }
        }

    /**
     * Name of the module currently selected
     */
    val currentModuleName
        get() = facet.module.name

    var searchString: String by Delegates.observable("") { _, old, new ->
        if (new != old) {
            filterOptions.searchString = new
        }
    }

    override fun getDirectories(): Array<PsiDirectory> =
        SourceProviderManager.getInstance(facet).mainIdeaSourceProvider.resDirectories.mapNotNull {
            runReadAction {
                PsiManager.getInstance(facet.module.project).findDirectory(it)
            }
        }.toTypedArray()

    override fun getOrChooseDirectory() = DirectoryChooserUtil.getOrChooseDirectory(this)

    override fun getData(dataId: String): Any? = when (dataId) {
        CommonDataKeys.PROJECT.name -> facet.module.project
        PlatformCoreDataKeys.MODULE.name -> facet.module
        LangDataKeys.IDE_VIEW.name -> this
        PlatformCoreDataKeys.BGT_DATA_PROVIDER.name -> DataProvider { getDataInBackground(it) }
        else -> null
    }

    private fun getDataInBackground(dataId: String): Any? = when (dataId) {
        CommonDataKeys.PSI_ELEMENT.name -> getPsiDirForDesignSystemType()
        else -> null
    }

    /**
     * Return the [AnAction]s to switch to another module.
     * This method only returns Android modules.
     */
    fun getAvailableModules(): List<String> = findCompatibleFacets(facet.module.project).map { it.module.name }.sorted()

    /**
     * Calls [facetUpdaterCallback] when a new module is selected in the ComboBox.
     */
    fun onModuleSelected(moduleName: String?) {
        getFacetForModuleName(moduleName, facet.module.project)?.run(facetUpdaterCallback)
    }

    /**
     * Returns one of the existing directories used for the current [DesignSystemType], or the default 'res' directory.
     *
     * Needed for AssetStudio.
     */
    private fun getPsiDirForDesignSystemType(): PsiDirectory? {
//        val resDirs = SourceProviderManager.getInstance(facet).mainIdeaSourceProvider.resDirectories
//        val subDir = FolderTypeRelationship.getRelatedFolders(resourceType).firstOrNull()?.let { resourceFolderType ->
//            getResourceSubdirs(resourceFolderType, resDirs).firstOrNull()
//        }
//        return (subDir ?: resDirs.firstOrNull())?.let { PsiManager.getInstance(facet.module.project).findDirectory(it) }
        return null
    }
}