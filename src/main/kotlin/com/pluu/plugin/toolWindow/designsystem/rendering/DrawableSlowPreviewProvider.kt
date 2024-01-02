package com.pluu.plugin.toolWindow.designsystem.rendering

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
    private val contextFile: VirtualFile?
) : SlowResourcePreviewProvider {

    override val previewPlaceholder: BufferedImage =
        createDrawablePlaceholderImage(JBUIScale.scale(20), JBUIScale.scale(20))

    override fun getSlowPreview(width: Int, height: Int, asset: DesignSystemItem): BufferedImage? {
        val configContext = contextFile ?: asset.file ?: return null
        val dimension = Dimension(width, height)
        val file = asset.file ?: return null
        return DesignAssetRendererManager.getInstance().getViewer(file)
            .getImage(file, facet.module, dimension, configContext).get()
    }
}