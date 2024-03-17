package com.pluu.plugin.toolWindow.designsystem.model

import com.intellij.ide.PasteProvider
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.actions.PasteAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinFileType
import java.awt.datatransfer.Transferable

class ResourcePasteProvider : PasteProvider {

    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        return isPastePossible(dataContext)
    }

    override fun isPastePossible(dataContext: DataContext): Boolean {
        return getTransferable(dataContext)
            ?.isDataFlavorSupported(DESIGN_SYSTEM_URL_FLAVOR) ?: false
    }

    override fun performPaste(dataContext: DataContext) {
        val caret = CommonDataKeys.CARET.getData(dataContext) ?: return
        val psiFile: PsiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return
        if (isPastePossible(psiFile)) {
            performForCode(dataContext, psiFile.fileType, caret)
        }
    }

    private fun isPastePossible(psiFile: PsiFile): Boolean {
        return when (psiFile.fileType) {
            XmlFileType.INSTANCE,
            KotlinFileType.INSTANCE -> true
            else -> false
        }
    }

    private fun performForCode(
        dataContext: DataContext,
        fileType: FileType,
        caret: Caret
    ) {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return
        val item = getDesignSystemItem(dataContext) ?: return
        if (!isPastSupport(fileType, item.applicableFileType)) return
        val sampleCode = item.sampleCode?.takeIf { it.isNotEmpty() } ?: return
        pasteAtCaret(caret, sampleCode, project)
    }

    private fun pasteAtCaret(caret: Caret, text: String, project: Project) {
        runWriteAction {
            caret.editor.document.insertString(caret.offset, text)
        }
        caret.selectStringFromOffset(text, caret.offset)
//
//        // Reformat code
//        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(caret.editor.document) ?: return
//        ReformatCodeProcessor(
//            psiFile,
//            caret.editor.selectionModel
//        ).run()
    }

    private fun getDesignSystemItem(dataContext: DataContext): DesignSystemItem? {
        return getTransferable(dataContext)?.getTransferData(DESIGN_SYSTEM_URL_FLAVOR) as? DesignSystemItem
    }

    private fun getTransferable(dataContext: DataContext): Transferable? {
        return PasteAction.TRANSFERABLE_PROVIDER.getData(dataContext)?.produce()
    }

    private fun isPastSupport(sourceFileType: FileType, sampleCodeType: ApplicableFileType): Boolean {
        return when (sampleCodeType) {
            ApplicableFileType.XML -> sourceFileType == XmlFileType.INSTANCE
            ApplicableFileType.KOTLIN -> sourceFileType == KotlinFileType.INSTANCE
            else -> false
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

private fun Caret.selectStringFromOffset(resourceReference: String, offset: Int) {
    setSelection(offset, offset + resourceReference.length)
    moveToOffset(offset + resourceReference.length)
}