package com.pdfreader.app.presentation.mvi

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfreader.app.domain.repository.PdfEngine
import com.pdfreader.app.domain.repository.PdfSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/**
 * ViewModel managing the PDF Reader state and processing intents.
 * Extends AndroidViewModel to easily access ContentResolver for file descriptors.
 */
class PdfReaderViewModel(
    application: Application,
    private val pdfEngine: PdfEngine,
    private val syncManager: PdfSyncManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PdfReaderState())
    val state: StateFlow<PdfReaderState> = _state.asStateFlow()

    fun processIntent(intent: PdfReaderIntent) {
        when (intent) {
            is PdfReaderIntent.OpenPdf -> openPdf(intent.uri)
            is PdfReaderIntent.ClosePdf -> closePdf()
            is PdfReaderIntent.SyncPdf -> syncPdf(intent.localFile)
            is PdfReaderIntent.RequestPageRender -> renderPage(
                intent.pageIndex,
                intent.width,
                intent.height,
                intent.onRendered
            )
            is PdfReaderIntent.SelectTool -> selectTool(intent.tool)
            is PdfReaderIntent.SelectPenColor -> selectPenColor(intent.index)
            is PdfReaderIntent.SelectHighlighterColor -> selectHighlighterColor(intent.index)
            is PdfReaderIntent.ToggleAnnotationSettings -> toggleAnnotationSettings()
            is PdfReaderIntent.SavePenColors -> savePenColors(intent.colors)
            is PdfReaderIntent.SaveHighlighterColors -> saveHighlighterColors(intent.colors)
            is PdfReaderIntent.AddStroke -> addStroke(intent.stroke)
            is PdfReaderIntent.RemoveStrokeAt -> removeStrokeAt(intent.pageIndex, intent.position)
            is PdfReaderIntent.AddTextAnnotation -> addTextAnnotation(intent.pageIndex, intent.position)
            is PdfReaderIntent.UpdateTextAnnotation -> updateTextAnnotation(intent.annotationId, intent.text)
        }
    }

    private fun selectTool(tool: AnnotationTool) {
        _state.update { it.copy(activeTool = tool, isAnnotationSettingsOpen = false) }
    }

    private fun selectPenColor(index: Int) {
        _state.update { state ->
            val safeIndex = index.coerceIn(0, state.penPalette.colors.lastIndex)
            state.copy(selectedPenColorIndex = safeIndex)
        }
    }

    private fun selectHighlighterColor(index: Int) {
        _state.update { state ->
            val safeIndex = index.coerceIn(0, state.highlighterPalette.colors.lastIndex)
            state.copy(selectedHighlighterColorIndex = safeIndex)
        }
    }

    private fun toggleAnnotationSettings() {
        _state.update { it.copy(isAnnotationSettingsOpen = !it.isAnnotationSettingsOpen) }
    }

    private fun savePenColors(colors: List<Long>) {
        if (colors.size != 4) return
        _state.update { state ->
            state.copy(
                penPalette = AnnotationPalette(colors),
                selectedPenColorIndex = state.selectedPenColorIndex.coerceIn(0, colors.lastIndex)
            )
        }
    }

    private fun saveHighlighterColors(colors: List<Long>) {
        if (colors.size != 4) return
        _state.update { state ->
            state.copy(
                highlighterPalette = AnnotationPalette(colors),
                selectedHighlighterColorIndex = state.selectedHighlighterColorIndex.coerceIn(0, colors.lastIndex)
            )
        }
    }

    private fun addStroke(stroke: FreehandStroke) {
        _state.update { state ->
            val pageStrokes = state.strokesByPage[stroke.pageIndex].orEmpty()
            state.copy(
                strokesByPage = state.strokesByPage + (stroke.pageIndex to (pageStrokes + stroke))
            )
        }
    }

    private fun removeStrokeAt(pageIndex: Int, position: androidx.compose.ui.geometry.Offset) {
        _state.update { state ->
            val pageStrokes = state.strokesByPage[pageIndex].orEmpty()
            val remaining = pageStrokes.filterNot { stroke ->
                stroke.points.any { point -> hypot(point.x - position.x, point.y - position.y) <= 0.035f }
            }
            state.copy(strokesByPage = state.strokesByPage + (pageIndex to remaining))
        }
    }

    private fun addTextAnnotation(pageIndex: Int, position: androidx.compose.ui.geometry.Offset) {
        _state.update { state ->
            val pageAnnotations = state.textAnnotationsByPage[pageIndex].orEmpty()
            val annotation = TextAnnotation(
                id = System.currentTimeMillis(),
                pageIndex = pageIndex,
                position = position,
                color = state.penPalette.colors[state.selectedPenColorIndex],
                text = ""
            )
            state.copy(textAnnotationsByPage = state.textAnnotationsByPage + (pageIndex to (pageAnnotations + annotation)))
        }
    }

    private fun updateTextAnnotation(annotationId: Long, text: String) {
        _state.update { state ->
            val updatedPages = state.textAnnotationsByPage.mapValues { (_, annotations) ->
                annotations.map { annotation ->
                    if (annotation.id == annotationId) annotation.copy(text = text) else annotation
                }
            }
            state.copy(textAnnotationsByPage = updatedPages)
        }
    }

    private fun syncPdf(localFile: java.io.File) {
        val uri = _state.value.openedUri ?: return
        _state.update { it.copy(isSyncing = true, errorMessage = null) }
        
        viewModelScope.launch(Dispatchers.IO) {
            val success = syncManager.syncBackToSource(uri, localFile)
            if (!success) {
                _state.update { it.copy(isSyncing = false, errorMessage = "Failed to sync to cloud provider.") }
            } else {
                _state.update { it.copy(isSyncing = false) }
            }
        }
    }

    private fun openPdf(uri: Uri) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                
                if (pfd != null) {
                    pdfEngine.openDocument(pfd)
                    val pageCount = pdfEngine.getPageCount()
                    
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            isPdfLoaded = true,
                            pageCount = pageCount,
                            openedUri = uri
                        ) 
                    }
                } else {
                    _state.update { 
                        it.copy(isLoading = false, errorMessage = "Failed to open file descriptor.") 
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { 
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error opening PDF") 
                }
            }
        }
    }

    private fun renderPage(pageIndex: Int, width: Int, height: Int, onRendered: (Bitmap?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure valid dimensions
                if (width <= 0 || height <= 0) {
                    withContext(Dispatchers.Main) { onRendered(null) }
                    return@launch
                }
                
                // For optimal performance and aspect ratio, we calculate the page size first
                val pageSize = pdfEngine.getPageSize(pageIndex)
                
                // Simple scaling logic to fit width
                val aspectRatio = pageSize.height.toFloat() / pageSize.width.toFloat()
                val targetHeight = (width * aspectRatio).toInt()
                
                val bitmap = pdfEngine.renderPage(pageIndex, width, targetHeight)
                
                withContext(Dispatchers.Main) {
                    onRendered(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onRendered(null)
                }
            }
        }
    }

    private fun closePdf() {
        pdfEngine.closeDocument()
        _state.update { PdfReaderState() }
    }

    override fun onCleared() {
        super.onCleared()
        pdfEngine.closeDocument()
    }
}
