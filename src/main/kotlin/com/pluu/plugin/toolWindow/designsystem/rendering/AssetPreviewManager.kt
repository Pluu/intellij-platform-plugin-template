package com.pluu.plugin.toolWindow.designsystem.rendering

import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import java.awt.Component
import javax.swing.Icon
import javax.swing.ImageIcon

interface DesignAssetPreviewManager {
    fun getPreviewProvider(resourceType: DesignSystemType): DesignAssetIconProvider
}

class DesignAssetPreviewManagerImpl(
    imageCache: ImageCache,
    private val contextFile: VirtualFile? = null
) : DesignAssetPreviewManager {
    private val drawablePreviewProvider by lazy {
        SlowDesignResourcePreviewManager(imageCache, DrawableSlowPreviewProvider(contextFile))
    }

    override fun getPreviewProvider(resourceType: DesignSystemType): DesignAssetIconProvider {
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

/**
 * An [DesignAssetIconProvider] that always returns an empty icon.
 */
class DefaultIconProvider private constructor() : DesignAssetIconProvider {
    companion object {
        val INSTANCE = DefaultIconProvider()
    }

    var icon: Icon = ImageIcon(EMPTY_IMAGE)

    override val supportsTransparency: Boolean = false

    override fun getIcon(
        assetToRender: DesignSystemItem,
        width: Int,
        height: Int,
        component: Component,
        refreshCallback: () -> Unit,
        shouldBeRendered: () -> Boolean
    ) = icon
}
