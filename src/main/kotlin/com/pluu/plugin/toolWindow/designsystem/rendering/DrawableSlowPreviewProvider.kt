package com.pluu.plugin.toolWindow.designsystem.rendering

import com.android.ide.common.resources.ResourceResolver
import com.android.tools.idea.ui.resourcemanager.plugin.DesignAssetRendererManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.scale.JBUIScale
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet
import java.awt.Dimension
import java.awt.image.BufferedImage

/**
 * [SlowResourcePreviewProvider] for Drawable and Mipmap resources.
 */
class DrawableSlowPreviewProvider(
    private val facet: AndroidFacet,
    private val resourceResolver: ResourceResolver,
    private val contextFile: VirtualFile?
) : SlowResourcePreviewProvider {
    private val project = facet.module.project

    override val previewPlaceholder: BufferedImage =
        createDrawablePlaceholderImage(JBUIScale.scale(20), JBUIScale.scale(20))

    override fun getSlowPreview(width: Int, height: Int, designAsset: DesignSystemItem): BufferedImage? {
        val configContext = contextFile ?: designAsset.file
        val dimension = Dimension(width, height)
        val file = designAsset.file
        return DesignAssetRendererManager.getInstance().getViewer(file)
            .getImage(file, facet.module, dimension, configContext).get()
    }
}