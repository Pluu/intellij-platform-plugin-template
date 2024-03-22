package com.pluu.plugin.toolWindow.designsystem.model

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/ui/resourcemanager/model/ResourcePasteProvider.kt
///////////////////////////////////////////////////////////////////////////

import com.intellij.codeInsight.actions.ReformatCodeProcessor
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
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.KotlinFileType

class ResourcePasteProvider : PasteProvider {
    override fun performPaste(dataContext: DataContext) {
        val caret = CommonDataKeys.CARET.getData(dataContext) ?: return
        val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return
        when (psiFile.fileType) {
            XmlFileType.INSTANCE,
            KotlinFileType.INSTANCE -> {
                performForCode(dataContext, psiFile.fileType, caret)
            }
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

        // Reformat code
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(caret.editor.document) ?: return
        ReformatCodeProcessor(
            psiFile,
            caret.editor.selectionModel
        ).run()
    }

    override fun isPastePossible(dataContext: DataContext): Boolean {
        return PasteAction.TRANSFERABLE_PROVIDER.getData(dataContext)
            ?.produce()
            ?.isDataFlavorSupported(DESIGN_SYSTEM_URL_FLAVOR) ?: false
    }

    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        return isPastePossible(dataContext)
    }

    private fun getDesignSystemItem(dataContext: DataContext): DesignSystemItem? =
        PasteAction.TRANSFERABLE_PROVIDER.getData(dataContext)
            ?.produce()
            ?.getTransferData(DESIGN_SYSTEM_URL_FLAVOR) as? DesignSystemItem

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun isPastSupport(sourceFileType: FileType, sampleCodeType: ApplicableFileType): Boolean {
        return when (sampleCodeType) {
            ApplicableFileType.XML -> sourceFileType == XmlFileType.INSTANCE
            ApplicableFileType.KOTLIN -> sourceFileType == KotlinFileType.INSTANCE
            else -> false
        }
    }
}

private fun Caret.selectStringFromOffset(resourceReference: String, offset: Int) {
    setSelection(offset, offset + resourceReference.length)
    moveToOffset(offset + resourceReference.length)
}