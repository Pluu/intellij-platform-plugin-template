package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.ide.PasteProvider
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.actions.PasteAction

class ResourcePasteProvider : PasteProvider {
    override fun performPaste(dataContext: DataContext) {
        val caret = CommonDataKeys.CARET.getData(dataContext) ?: return
        val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return
        if (psiFile.fileType != XmlFileType.INSTANCE) {
            return
        }
        performForXml(dataContext, caret)
    }

    /**
     * Perform the paste in an XML file context.
     * The paste operation will be different depending on the psiElement under the caret.
     *
     * For example, if the caret is within an ImageView tag, the `src` attribute will be populated with the
     * pasted [DesignSystemItem].
     */
    private fun performForXml(
        dataContext: DataContext,
        caret: Caret
    ) {
        val resourceUrl = getResourceUrl(dataContext) ?: return
        val resourceReference = resourceUrl.file.name
        pasteAtCaret(caret, resourceReference)
    }

    private fun pasteAtCaret(
        caret: Caret,
        resourceReference: String
    ) {
        runWriteAction {
            caret.editor.document.insertString(caret.offset, resourceReference)
        }
        caret.selectStringFromOffset(resourceReference, caret.offset)
    }

    override fun isPastePossible(dataContext: DataContext): Boolean {
        return PasteAction.TRANSFERABLE_PROVIDER.getData(dataContext)
            ?.produce()
            ?.isDataFlavorSupported(DESIGN_SYSTEM_URL_FLAVOR) ?: false
    }

    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        return isPastePossible(dataContext)
    }

    private fun getResourceUrl(dataContext: DataContext): DesignSystemItem? =
        PasteAction.TRANSFERABLE_PROVIDER.getData(dataContext)
            ?.produce()
            ?.getTransferData(DESIGN_SYSTEM_URL_FLAVOR) as? DesignSystemItem

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

private fun Caret.selectStringFromOffset(resourceReference: String, offset: Int) {
    setSelection(offset, offset + resourceReference.length)
    moveToOffset(offset + resourceReference.length)
}