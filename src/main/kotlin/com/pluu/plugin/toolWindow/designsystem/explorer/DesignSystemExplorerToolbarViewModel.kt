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
import com.pluu.plugin.toolWindow.designsystem.model.FilterImageSize
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.model.TypeFiltersModel
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

    /** View callback whenever the resources lists needs to be repopulated. */
    var populateResourcesCallback: (() -> Unit) = {}

    /** Called when a new facet is selected. */
    var facetUpdaterCallback: (AndroidFacet) -> Unit = {}

    /** Callback for when a request to refresh resources previews is made. */
    var refreshResourcesPreviewsCallback: () -> Unit = {}

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

    var searchString: String by Delegates.observable("") { _, old, new ->
        if (new != old) {
            filterOptions.searchString = new
        }
    }

    var sampleImageSize: FilterImageSize
        get() = filterOptions.sampleImageSize
        set(value) {
            filterOptions.sampleImageSize = value
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

    var typeFiltersModel: TypeFiltersModel = filterOptions.typeFiltersModel

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