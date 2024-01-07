package com.pluu.plugin.toolWindow.designsystem.qualifiers

import com.pluu.plugin.toolWindow.designsystem.DesignSystemType

class QualifierConfigurationViewModel {
    val designSystemTypes: Array<DesignSystemType>
        get() = DesignSystemType.values()
}