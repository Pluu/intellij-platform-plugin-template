package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

// Icon format : https://plugins.jetbrains.com/docs/intellij/icons.html#icon-formats
object PluuIcons {
    fun load(path: String): Icon {
        return IconLoader.getIcon(path, javaClass)
    }

    @JvmField
    val Konata: Icon = load("/icons/konata.png")
}

