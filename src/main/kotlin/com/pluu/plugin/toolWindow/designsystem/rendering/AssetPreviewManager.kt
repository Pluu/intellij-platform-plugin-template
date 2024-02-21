package com.pluu.plugin.toolWindow.designsystem.rendering

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/rendering/AssetPreviewManager.kt
///////////////////////////////////////////////////////////////////////////

import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.awt.Component
import javax.swing.Icon

interface DesignAssetPreviewManager {
    fun getPreviewProvider(): DesignAssetIconProvider
}

class DesignAssetPreviewManagerImpl(
    imageCache: ImageCache
) : DesignAssetPreviewManager {
    private val drawablePreviewProvider by lazy {
        SlowDesignResourcePreviewManager(imageCache, DrawableSlowPreviewProvider())
    }

    override fun getPreviewProvider(): DesignAssetIconProvider {
        return drawablePreviewProvider
    }
}

interface DesignAssetIconProvider {
    val supportsTransparency: Boolean

    fun getIcon(
        assetToRender: DesignSystemItem,
        width: Int,
        height: Int,
        component: Component,
        refreshCallback: () -> Unit = {},
        shouldBeRendered: () -> Boolean = { true }
    ): Icon
}
