package com.pluu.plugin.toolWindow.designsystem.model

import icons.PluuIcons
import javax.swing.Icon

data class DesignSystemType(
    val name: String,
    val category: CategoryType,
    val icon: Icon
) {
    companion object {
        val NONE = DesignSystemType("", CategoryType.Etc, DesignSystemIcon.None)

        fun default(name: String) = DesignSystemType(name, CategoryType.Etc, DesignSystemIcon.None)

        val defaultTypes: List<DesignSystemType> by lazy {
            listOf(
                DesignSystemType("AppBar", CategoryType.Layout, DesignSystemIcon.AppBar),
                DesignSystemType("Badge", CategoryType.Badge, DesignSystemIcon.Badge),
                DesignSystemType("Button", CategoryType.Button, DesignSystemIcon.Button),
                DesignSystemType("Card", CategoryType.Card, DesignSystemIcon.Card),
                DesignSystemType("CheckBox", CategoryType.Button, DesignSystemIcon.CheckBox),
                DesignSystemType("Chip", CategoryType.Control, DesignSystemIcon.Chip),
                DesignSystemType("Etc", CategoryType.Etc, DesignSystemIcon.None),
                DesignSystemType("Image", CategoryType.Image, DesignSystemIcon.Image),
                DesignSystemType("Input", CategoryType.Text, DesignSystemIcon.Text),
                DesignSystemType("RadioButton", CategoryType.Button, DesignSystemIcon.RadioButton),
                DesignSystemType("Scaffold", CategoryType.Layout, DesignSystemIcon.Scaffold),
                DesignSystemType("Slider", CategoryType.Control, DesignSystemIcon.Slider),
                DesignSystemType("Switch", CategoryType.Control, DesignSystemIcon.Switch),
                DesignSystemType("Text", CategoryType.Text, DesignSystemIcon.Text),
                DesignSystemType("Toast", CategoryType.Toast, DesignSystemIcon.Toast),
            )
        }
    }
}

enum class CategoryType {
    Text,
    Button,
    Badge,
    Card,
    Control,
    Image,
    Layout,
    Toast,
    Etc,
}

object DesignSystemIcon {
    val AppBar = PluuIcons.load("/icons/icon_app_bar.svg")
    val Badge = PluuIcons.load("/icons/icon_button.svg")
    val Button = PluuIcons.load("/icons/icon_button.svg")
    val Card = PluuIcons.load("/icons/icon_card")
    val CheckBox = PluuIcons.load("/icons/icon_check_box.svg")
    val Chip = PluuIcons.load("/icons/icon_chip.svg")
    val Image = PluuIcons.load("/icons/icon_image.svg")
    val None = PluuIcons.load("/icons/icon_none.svg")
    val RadioButton = PluuIcons.load("/icons/icon_radio_button.svg")
    val Scaffold = PluuIcons.load("/icons/icon_app_bar.svg")
    val Slider = PluuIcons.load("/icons/icon_slider.svg")
    val Switch = PluuIcons.load("/icons/icon_toggle.svg")
    val Text = PluuIcons.load("/icons/icon_text.svg")
    val Toast = PluuIcons.load("/icons/icon_toast.svg")
}
