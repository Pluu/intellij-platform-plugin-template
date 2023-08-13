package com.pluu.plugin.codeInsight.postfix

///////////////////////////////////////////////////////////////////////////
// https://cs.android.com/android-studio/intellij-kotlin/+/master:idea/src/org/jetbrains/kotlin/idea/codeInsight/postfix/KtPostfixTemplateProvider.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil.findElementOfClassAtRange
import com.intellij.util.Function
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.safeAnalyze
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.idea.util.isAnonymousFunction
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

internal fun createExpressionSelector(
    checkCanBeUsedAsValue: Boolean = true,
    statementsOnly: Boolean = false,
    typePredicate: ((KotlinType) -> Boolean)? = null
): PostfixTemplateExpressionSelector {
    val predicate: ((KtExpression, BindingContext) -> Boolean)? =
        if (typePredicate != null) { expression, bindingContext ->
            expression.getType(bindingContext)?.let(typePredicate) ?: false
        }
        else null
    return createExpressionSelectorWithComplexFilter(checkCanBeUsedAsValue, statementsOnly, predicate)
}

internal fun createExpressionSelectorWithComplexFilter(
    // Do not suggest expressions like 'val a = 1'/'for ...'
    checkCanBeUsedAsValue: Boolean = true,
    statementsOnly: Boolean = false,
    predicate: ((KtExpression, BindingContext) -> Boolean)? = null
): PostfixTemplateExpressionSelector = KtExpressionPostfixTemplateSelector(checkCanBeUsedAsValue, statementsOnly, predicate)

private class KtExpressionPostfixTemplateSelector(
    private val checkCanBeUsedAsValue: Boolean,
    private val statementsOnly: Boolean,
    private val predicate: ((KtExpression, BindingContext) -> Boolean)?
) : PostfixTemplateExpressionSelector {

    private fun filterElement(element: PsiElement): Boolean {
        if (element !is KtExpression) return false

        if (element.parent is KtThisExpression) return false

        // Can't be independent expressions
        if (element.isSelector || element.parent is KtUserType || element.isOperationReference || element is KtBlockExpression) return false

        // Both KtLambdaExpression and KtFunctionLiteral have the same offset, so we add only one of them -> KtLambdaExpression
        if (element is KtFunctionLiteral) return false

        if (statementsOnly) {
            // We use getQualifiedExpressionForReceiverOrThis because when postfix completion is run on some statement like:
            // foo().try<caret>
            // `element` points to `foo()` call, while we need to select the whole statement with `try` selector
            // to check if it's in a statement position
            if (!KtPsiUtil.isStatement(element.getQualifiedExpressionForReceiverOrThis())) return false
        }
        if (checkCanBeUsedAsValue && !element.canBeUsedAsValue()) return false

        return predicate?.invoke(element, element.safeAnalyze(element.getResolutionFacade(), BodyResolveMode.PARTIAL)) ?: true
    }

    private fun KtExpression.canBeUsedAsValue() =
        !KtPsiUtil.isAssignment(this) &&
                !this.isNamedDeclaration &&
                this !is KtLoopExpression &&
                // if's only with else may be treated as expressions
                !isIfWithoutElse

    private val KtExpression.isIfWithoutElse: Boolean
        get() = (this is KtIfExpression && this.elseKeyword == null)

    override fun getExpressions(context: PsiElement, document: Document, offset: Int): List<PsiElement> {
        val originalFile = context.containingFile.originalFile
        val textRange = context.textRange
        val originalElement = findElementOfClassAtRange(originalFile, textRange.startOffset, textRange.endOffset, context::class.java)
            ?: return emptyList()

        val expressions = originalElement.parentsWithSelf
            .filterIsInstance<KtExpression>()
            .takeWhile { !it.isBlockBodyInDeclaration }

        val boundExpression = expressions.firstOrNull { it.parent.endOffset > offset }
        val boundElementParent = boundExpression?.parent
        val filteredByOffset = expressions.takeWhile { it != boundElementParent }.toMutableList()
        if (boundElementParent is KtDotQualifiedExpression && boundExpression == boundElementParent.receiverExpression) {
            val qualifiedExpressionEnd = boundElementParent.endOffset
            expressions
                .dropWhile { it != boundElementParent }
                .drop(1)
                .takeWhile { it.endOffset == qualifiedExpressionEnd }
                .toCollection(filteredByOffset)
        }

        val result = filteredByOffset.filter(this::filterElement)

        if (isUnitTestMode() && result.size > 1) {
            PluuPostfixTemplateProvider.previouslySuggestedExpressions = result.map { it.text }
        }

        return result
    }

    override fun hasExpression(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean =
        getExpressions(context, copyDocument, newOffset).isNotEmpty()

    override fun getRenderer() = Function(PsiElement::getText)
}

private val KtExpression.isOperationReference: Boolean
    get() = this.node.elementType == KtNodeTypes.OPERATION_REFERENCE

private val KtElement.isBlockBodyInDeclaration: Boolean
    get() = this is KtBlockExpression && (parent as? KtElement)?.isNamedDeclarationWithBody == true

private val KtElement.isNamedDeclaration: Boolean
    get() = this is KtNamedDeclaration && !isAnonymousFunction

private val KtElement.isNamedDeclarationWithBody: Boolean
    get() = this is KtDeclarationWithBody && !isAnonymousFunction

private val KtDeclaration.isAnonymousFunction: Boolean
    get() = this is KtFunctionLiteral || (this is KtNamedFunction && this.name == null)

private val KtExpression.isSelector: Boolean
    get() = parent is KtQualifiedExpression && (parent as KtQualifiedExpression).selectorExpression == this
