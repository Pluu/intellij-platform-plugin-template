package com.pluu.plugin.toolWindow.designsystem.rendering

import com.android.ide.common.resources.ResourceResolver
import com.intellij.openapi.vfs.VirtualFile
import com.pluu.plugin.toolWindow.designsystem.DesignSystemType
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.android.facet.AndroidFacet
import java.awt.Component
import javax.swing.Icon
import javax.swing.ImageIcon

interface DesignAssetPreviewManager {
    fun getPreviewProvider(resourceType: DesignSystemType): DesignAssetIconProvider
}

class DesignAssetPreviewManagerImpl(
    private val facet: AndroidFacet,
    imageCache: ImageCache,
    private val resourceResolver: ResourceResolver,
    private val contextFile: VirtualFile? = null
) : DesignAssetPreviewManager {
    private val drawablePreviewProvider by lazy {
        SlowDesignResourcePreviewManager(imageCache, DrawableSlowPreviewProvider(facet, resourceResolver, contextFile))
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
 * An [AssetIconProvider] that always returns an empty icon.
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
