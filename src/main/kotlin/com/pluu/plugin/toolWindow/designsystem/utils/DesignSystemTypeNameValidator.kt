package com.pluu.plugin.toolWindow.designsystem.utils

object DesignSystemTypeNameValidator {
    fun getErrorTextForFileResource(text: String): String? {
        if (text.isEmpty() || !Character.isJavaIdentifierStart(text.first())) {
            return "Name must begin with a letter"
        }
        return null
    }

    fun isValidCharacter(c: Int): Boolean {
        return (c >= 'a'.code && c <= 'z'.code) ||
                (c >= 'A'.code && c <= 'Z'.code) ||
                (c >= '0'.code && c <= '9'.code) ||
                (c == '_'.code)
    }
}