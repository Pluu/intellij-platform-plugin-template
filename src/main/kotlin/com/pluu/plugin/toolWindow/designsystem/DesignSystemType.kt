package com.pluu.plugin.toolWindow.designsystem

enum class DesignSystemType(val displayName: String) {
    NONE(""),
    INPUT("Input"),
    BUTTON("Button"),
    TYPE1("Type1"),
    TYPE2("Type2"),
    TYPE3("Type3");

    fun isSelectable(): Boolean = this != NONE

    companion object {
        fun selectableTypes(): Array<DesignSystemType> = values()
            .filter { it.isSelectable() }
            .toTypedArray()

        val defaultType: DesignSystemType = BUTTON
    }
}

