package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

// Icon format : https://plugins.jetbrains.com/docs/intellij/icons.html#icon-formats
object PluuIcons {
    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, javaClass)
    }

    @JvmField
    val Konata: Icon = load("/icons/konata.png")

    val iconButton = load("/icons/icon_button.svg")
    val iconSlider = load("/icons/icon_slider.svg")
    val iconText = load("/icons/icon_text.svg")
    val iconToast = load("/icons/icon_toast.svg")
    val iconNone = load("/icons/icon_none.svg")
}

