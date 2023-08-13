package com.pluu.plugin.codeInsight.postfix

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.impl.MacroCallNode
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.liveTemplates.macro.KotlinClassNameMacro

internal class KtAttributeSet2(
    provider: PostfixTemplateProvider
) : StringBasedPostfixTemplate(
    "obtainStyledAttributes",
    /* example = */ "attrs.obtainStyledAttributes",
    /* selector = */ createExpressionSelector { type ->
        type.fqName?.asString() == "android.util.AttributeSet"
    },
    /* provider = */ provider
) {
    override fun setVariables(template: Template, element: PsiElement) {
        super.setVariables(template, element)
        template.addVariable("name", MacroCallNode(KotlinClassNameMacro()), false)
    }

    override fun getTemplateString(p0: PsiElement) =
        "context.obtainStyledAttributes(\$expr\$, R.styleable.\$name\$, defStyle, 0).use {\n\$END\$\n}"

    override fun getElementToRemove(expr: PsiElement?) = expr
}
