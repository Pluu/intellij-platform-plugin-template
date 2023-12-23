package com.pluu.plugin.toolWindow.designsystem.model

import com.pluu.plugin.toolWindow.designsystem.DesignSystemType

data class DesignSection(
    val type: DesignSystemType,
    val assetSets: List<DesignAssetSet>
)
