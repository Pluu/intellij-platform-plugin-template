package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object PluuIcons {
    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, javaClass)
    }

    @JvmField
    val Konata: Icon = load("/icons/konata.png")

    @JvmField
    val Konata_32: Icon = load("/icons/konata_32.png")

    @JvmField
    val Studio: Icon = load("icons/androidstudio_icon.svg")
}

