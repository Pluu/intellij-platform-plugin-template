package com.pluu.plugin.toolWindow.designsystem.explorer

///////////////////////////////////////////////////////////////////////////
// Origin ; https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/explorer/ResourceExplorerToolbarViewModel.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.projectsystem.SourceProviderManager
import com.intellij.ide.IdeView
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DataSnapshotProvider
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.pluu.plugin.toolWindow.designsystem.model.CategoryType
import com.pluu.plugin.toolWindow.designsystem.model.FilterImageSize
import com.pluu.plugin.toolWindow.designsystem.model.FilterOptions
import com.pluu.plugin.toolWindow.designsystem.model.TypeFiltersModel
import org.jetbrains.android.facet.AndroidFacet
import kotlin.properties.Delegates

class DesignSystemExplorerToolbarViewModel(
    facet: AndroidFacet,
    initialDesignSystemType: CategoryType?,
    private val filterOptions: FilterOptions
) : DataSnapshotProvider, IdeView {

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

    var requestSearch = {}

    var categoryType: CategoryType? by Delegates.observable(initialDesignSystemType) { _, oldValue, newValue ->
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

    var typeFiltersModel: TypeFiltersModel = filterOptions.typeFiltersModel

    override fun dataSnapshot(sink: DataSink) {
        sink[CommonDataKeys.PROJECT] = facet.module.project
        sink[PlatformCoreDataKeys.MODULE] = facet.module
        sink[LangDataKeys.IDE_VIEW] = this
        sink.lazy(CommonDataKeys.PSI_ELEMENT) {
            getPsiDirForResourceType()
        }
    }

    /**
     * Implementation of [IdeView.getDirectories] that returns the main resource directories of the current facet.
     *
     * Needed for AssetStudio.
     */
    override fun getDirectories(): Array<PsiDirectory> =
        SourceProviderManager.getInstance(facet).mainIdeaSourceProvider?.resDirectories?.mapNotNull {
            runReadAction {
                PsiManager.getInstance(facet.module.project).findDirectory(it)
            }
        }?.toTypedArray() ?: emptyArray()

    override fun getOrChooseDirectory() = DirectoryChooserUtil.getOrChooseDirectory(this)

    /**
     * Returns one of the existing directories used for the current [ResourceType], or the default 'res' directory.
     *
     * Needed for AssetStudio.
     */
    private fun getPsiDirForResourceType(): PsiDirectory? {
        val resDirs = SourceProviderManager.getInstance(facet).mainIdeaSourceProvider?.resDirectories ?: emptyList()
        return (resDirs.firstOrNull())?.let { PsiManager.getInstance(facet.module.project).findDirectory(it) }
    }
}