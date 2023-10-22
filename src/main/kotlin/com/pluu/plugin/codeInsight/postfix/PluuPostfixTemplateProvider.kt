package com.pluu.plugin.codeInsight.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly

class PluuPostfixTemplateProvider : PostfixTemplateProvider {

    override fun getTemplates(): Set<PostfixTemplate> = setOf(
        KtAttributeSet2(this)
    )

    override fun isTerminalSymbol(currentChar: Char): Boolean = currentChar == '.' || currentChar == '!'

    override fun preExpand(file: PsiFile, editor: Editor) {
    }

    override fun afterExpand(file: PsiFile, editor: Editor) {
    }

    override fun preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int): PsiFile = copyFile

    companion object {
        /**
         * In tests only one expression should be suggested, so in case there are many of them, save relevant items
         */
        @TestOnly
        @Volatile
        var previouslySuggestedExpressions = emptyList<String>()
    }
}
