package com.pluu.plugin.toolWindow.designsystem.rendering

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/rendering/DrawableSlowPreviewProvider.kt
///////////////////////////////////////////////////////////////////////////

import com.android.tools.idea.ui.resourcemanager.plugin.DesignAssetRendererManager
import com.intellij.ui.scale.JBUIScale
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.awt.Dimension
import java.awt.image.BufferedImage

/**
 * [SlowResourcePreviewProvider] for Drawable and Mipmap resources.
 */
class DrawableSlowPreviewProvider : SlowResourcePreviewProvider {

    override val previewPlaceholder: BufferedImage =
        createDrawablePlaceholderImage(JBUIScale.scale(20), JBUIScale.scale(20))

    override fun getSlowPreview(width: Int, height: Int, asset: DesignSystemItem): BufferedImage? {
        val dimension = Dimension(width, height)
        val file = asset.file ?: return null
        return DesignAssetRendererManager.getInstance().getViewer(file)
            .getImage(file, null, dimension, null).get()
    }
}