package com.pluu.plugin.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

class ConfigComponent {
    val root: JPanel

    private lateinit var designSystemStatus: JBCheckBox

    var isEnableDesignSystem: Boolean
        get() = designSystemStatus.isSelected
        set(value) {
            designSystemStatus.isSelected = value
        }

    init {
        root = panel {
            group("Pluu Plugin") {
                row {
                    designSystemStatus = checkBox("Enable Design System").component
                }
            }
        }
    }

}