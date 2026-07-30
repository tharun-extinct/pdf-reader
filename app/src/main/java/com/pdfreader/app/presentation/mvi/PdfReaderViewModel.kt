package com.pdfreader.app.presentation.mvi

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfreader.app.domain.model.ReaderPreferences
import com.pdfreader.app.domain.model.RecentDocument
import com.pdfreader.app.domain.repository.LibraryRepository
import com.pdfreader.app.data.pdfbox.PdfAnnotationWriterImpl
import com.pdfreader.app.domain.repository.PdfAnnotationSaver
import com.pdfreader.app.domain.repository.PdfEngine
import com.pdfreader.app.domain.repository.PdfSyncManager
import com.pdfreader.app.domain.tts.TtsManager
import com.pdfreader.app.domain.tts.TtsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val syncManager: PdfSyncManager,
    private val libraryRepository: LibraryRepository,
    private val annotationSaver: PdfAnnotationSaver = PdfAnnotationWriterImpl()
) : AndroidViewModel(application) {

    private val ttsManager = TtsManager(application)

    private val _state = MutableStateFlow(PdfReaderState())
    val state: StateFlow<PdfReaderState> = _state.asStateFlow()
    private var progressSaveJob: Job? = null
    private var preferencesSaveJob: Job? = null

    init {
        viewModelScope.launch {
            ttsManager.ttsState.collect { ttsState ->
                _state.update { it.copy(ttsState = ttsState) }
            }
        }
        loadLibrary()
    }

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
            is PdfReaderIntent.RequestPageText -> extractPageText(intent.pageIndex, intent.onExtracted)
            is PdfReaderIntent.PageChanged -> onPageChanged(intent.pageIndex)
            PdfReaderIntent.ToggleBookmark -> toggleBookmark()
            PdfReaderIntent.ClearRecentDocuments -> clearRecentDocuments()
            PdfReaderIntent.DismissError -> _state.update { it.copy(errorMessage = null) }
            is PdfReaderIntent.SetThemeMode -> updatePreferences { it.copy(themeMode = intent.mode) }
            is PdfReaderIntent.SetKeepScreenOn -> updatePreferences { it.copy(keepScreenOn = intent.enabled) }
            is PdfReaderIntent.SetSpeechRate -> {
                val rate = intent.rate.coerceIn(0.6f, 1.6f)
                ttsManager.setSpeechRate(rate)
                updatePreferences { it.copy(speechRate = rate) }
            }
            is PdfReaderIntent.RequestPageHighlights -> extractEmbeddedHighlights(intent.pageIndex, intent.onLoaded)
            is PdfReaderIntent.SelectTool -> selectTool(intent.tool)
            is PdfReaderIntent.SelectPenColor -> selectPenColor(intent.index)
            is PdfReaderIntent.SelectHighlighterColor -> selectHighlighterColor(intent.index)
            is PdfReaderIntent.ToggleAnnotationSettings -> toggleAnnotationSettings()
            is PdfReaderIntent.SavePenColors -> savePenColors(intent.colors)
            is PdfReaderIntent.SaveHighlighterColors -> saveHighlighterColors(intent.colors)
            is PdfReaderIntent.AddStroke -> addStroke(intent.stroke)
            is PdfReaderIntent.AddTextHighlight -> addTextHighlight(intent.highlight)
            is PdfReaderIntent.RemoveStrokeAt -> removeStrokeAt(intent.pageIndex, intent.position)
            is PdfReaderIntent.AddTextAnnotation -> addTextAnnotation(intent.pageIndex, intent.position)
            is PdfReaderIntent.UpdateTextAnnotation -> updateTextAnnotation(intent.annotationId, intent.text)
            is PdfReaderIntent.SetAnnotationSaveMode -> _state.update { it.copy(annotationSaveMode = intent.mode) }
            is PdfReaderIntent.SelectHighlightAt -> selectHighlightAt(intent.pageIndex, intent.position)
            is PdfReaderIntent.ClearHighlightSelection -> _state.update { it.copy(selectedHighlight = null) }
            is PdfReaderIntent.DeleteSelectedHighlight -> deleteSelectedHighlight()
            is PdfReaderIntent.PlayTts -> playTts(intent.pageIndex, intent.textBoxes)
            is PdfReaderIntent.PauseTts -> ttsManager.pause()
            is PdfReaderIntent.ResumeTts -> ttsManager.resume()
            is PdfReaderIntent.StopTts -> ttsManager.stop()
            is PdfReaderIntent.SaveAnnotations -> saveAnnotations()
        }
    }

    private fun loadLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val recentDocuments = libraryRepository.getRecentDocuments()
            val preferences = libraryRepository.getPreferences()
            withContext(Dispatchers.Main) {
                ttsManager.setSpeechRate(preferences.speechRate)
            }
            _state.update {
                it.copy(
                    isLibraryLoading = false,
                    recentDocuments = recentDocuments,
                    preferences = preferences
                )
            }
        }
    }

    private fun updatePreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        val updated = transform(_state.value.preferences)
        _state.update { it.copy(preferences = updated) }
        preferencesSaveJob?.cancel()
        preferencesSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(PREFERENCES_SAVE_DEBOUNCE_MS)
            libraryRepository.savePreferences(updated)
        }
    }

    private fun onPageChanged(pageIndex: Int) {
        val currentState = _state.value
        if (!currentState.isPdfLoaded || currentState.pageCount <= 0) return

        val safePageIndex = pageIndex.coerceIn(0, currentState.pageCount - 1)
        _state.update { it.copy(currentPageIndex = safePageIndex) }

        val uri = currentState.openedUri?.toString() ?: return
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(PROGRESS_SAVE_DEBOUNCE_MS)
            libraryRepository.updateProgress(uri, safePageIndex)
            val recentDocuments = libraryRepository.getRecentDocuments()
            _state.update { it.copy(recentDocuments = recentDocuments) }
        }
    }

    private fun toggleBookmark() {
        val currentState = _state.value
        val uri = currentState.openedUri?.toString() ?: return
        val pageIndex = currentState.currentPageIndex
        viewModelScope.launch(Dispatchers.IO) {
            val bookmarkedPages = libraryRepository.toggleBookmark(uri, pageIndex)
            val recentDocuments = libraryRepository.getRecentDocuments()
            _state.update {
                it.copy(
                    bookmarkedPages = bookmarkedPages,
                    recentDocuments = recentDocuments
                )
            }
        }
    }

    private fun clearRecentDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            libraryRepository.clearRecentDocuments()
            _state.update { it.copy(recentDocuments = emptyList()) }
        }
    }

    private fun playTts(pageIndex: Int, textBoxes: List<com.pdfreader.app.presentation.mvi.PdfTextBox>) {
        ttsManager.play(pageIndex, textBoxes)
    }

    private fun selectTool(tool: AnnotationTool) {
        val nextTool = if (_state.value.activeTool == tool) {
            AnnotationTool.None
        } else {
            tool
        }
        if (nextTool != AnnotationTool.ReadAloud) {
            ttsManager.stop()
        }
        _state.update { state ->
            state.copy(activeTool = nextTool, isAnnotationSettingsOpen = false)
        }
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

    private fun addTextHighlight(highlight: TextHighlight) {
        _state.update { state ->
            val pageHighlights = state.highlightsByPage[highlight.pageIndex].orEmpty()
            state.copy(
                highlightsByPage = state.highlightsByPage + (highlight.pageIndex to (pageHighlights + highlight))
            )
        }
    }

    private fun selectHighlightAt(pageIndex: Int, position: androidx.compose.ui.geometry.Offset) {
        _state.update { state ->
            val sessionCandidates = state.highlightsByPage[pageIndex].orEmpty().map {
                SelectedHighlight(
                    id = it.id.toString(),
                    pageIndex = pageIndex,
                    source = HighlightSource.Session,
                    color = it.color,
                    rects = it.rects
                )
            }
            val deletedIds = state.deletedEmbeddedHighlightIdsByPage[pageIndex].orEmpty()
            val embeddedCandidates = state.embeddedHighlightsByPage[pageIndex].orEmpty()
                .filterNot { it.id in deletedIds }
                .map {
                    SelectedHighlight(
                        id = it.id,
                        pageIndex = pageIndex,
                        source = HighlightSource.Embedded,
                        color = it.color,
                        rects = it.rects
                    )
                }
            val selected = HighlightHitTester.select(position, sessionCandidates + embeddedCandidates)
            state.copy(selectedHighlight = selected)
        }
    }

    private fun deleteSelectedHighlight() {
        _state.update { state ->
            val selected = state.selectedHighlight ?: return@update state
            when (selected.source) {
                HighlightSource.Session -> {
                    val id = selected.id.toLongOrNull()
                    val remaining = state.highlightsByPage[selected.pageIndex].orEmpty()
                        .filterNot { it.id == id }
                    state.copy(
                        highlightsByPage = state.highlightsByPage + (selected.pageIndex to remaining),
                        selectedHighlight = null
                    )
                }
                HighlightSource.Embedded -> {
                    val existing = state.deletedEmbeddedHighlightIdsByPage[selected.pageIndex].orEmpty()
                    state.copy(
                        deletedEmbeddedHighlightIdsByPage = state.deletedEmbeddedHighlightIdsByPage +
                            (selected.pageIndex to (existing + selected.id)),
                        selectedHighlight = null
                    )
                }
            }
        }
    }

    private fun removeStrokeAt(pageIndex: Int, position: androidx.compose.ui.geometry.Offset) {
        _state.update { state ->
            val pageStrokes = state.strokesByPage[pageIndex].orEmpty()
                val remainingStrokes = pageStrokes.filterNot { stroke ->
                    stroke.points.any { point -> hypot(point.x - position.x, point.y - position.y) <= 0.035f }
                }
                val pageHighlights = state.highlightsByPage[pageIndex].orEmpty()
                val remainingHighlights = pageHighlights.filterNot { highlight ->
                    highlight.rects.any { it.inflate(0.015f).contains(position) }
                }
            state.copy(
                strokesByPage = state.strokesByPage + (pageIndex to remainingStrokes),
                highlightsByPage = state.highlightsByPage + (pageIndex to remainingHighlights)
            )
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

    /**
     * Bakes all in-memory annotations into the PDF using PDFBox, writes the result
     * to a temp file in cacheDir, syncs it back to the source URI via SAF, then
     * clears the in-memory overlay and re-opens the document so the next render
     * reflects the now-embedded annotations.
     */
    private fun saveAnnotations() {
        val uri = _state.value.openedUri ?: return
        val pdfBytes = pdfEngine.getPdfBytes() ?: return
        val currentState = _state.value

        // Nothing to save
        val hasAnnotations = currentState.strokesByPage.values.any { it.isNotEmpty() } ||
            currentState.highlightsByPage.values.any { it.isNotEmpty() } ||
            currentState.textAnnotationsByPage.values.any { it.isNotEmpty() } ||
            currentState.deletedEmbeddedHighlightIdsByPage.values.any { it.isNotEmpty() }
        if (!hasAnnotations) return

        _state.update { it.copy(isSavingAnnotations = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            var tempFile: java.io.File? = null
            try {
                val context = getApplication<Application>().applicationContext
                val outputFile = java.io.File(context.cacheDir, "annotated_${System.currentTimeMillis()}.pdf")
                tempFile = outputFile

                // Write annotations into PDF structure
                annotationSaver.saveAnnotations(
                    pdfBytes = pdfBytes,
                    strokesByPage = currentState.strokesByPage,
                    highlightsByPage = currentState.highlightsByPage,
                    textAnnotationsByPage = currentState.textAnnotationsByPage,
                    deletedEmbeddedHighlightIdsByPage = currentState.deletedEmbeddedHighlightIdsByPage,
                    saveMode = currentState.annotationSaveMode,
                    outputFile = outputFile
                )

                // Sync annotated file back to the source URI (Google Drive, local, etc.)
                val success = syncManager.syncBackToSource(uri, outputFile)
                if (!success) {
                    _state.update {
                        it.copy(isSavingAnnotations = false, errorMessage = "Failed to save annotations to source.")
                    }
                    return@launch
                }

                // Re-open the document so PDFium renders the embedded annotations
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalStateException("Failed to reopen PDF after saving annotations.")
                val newBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Failed to re-read PDF after saving annotations.")
                pdfEngine.openDocument(pfd, newBytes)

                // Bump renderRevision: PdfPage composables observe this key and will
                // discard their cached bitmaps, triggering a fresh render that shows
                // the now-embedded annotations via PDFium.
                _state.update {
                    it.copy(
                        isSavingAnnotations = false,
                        strokesByPage = emptyMap(),
                        highlightsByPage = emptyMap(),
                        textAnnotationsByPage = emptyMap(),
                        embeddedHighlightsByPage = emptyMap(),
                        deletedEmbeddedHighlightIdsByPage = emptyMap(),
                        selectedHighlight = null,
                        textBoxesByPage = emptyMap(), // invalidated by document re-open
                        renderRevision = it.renderRevision + 1
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(isSavingAnnotations = false, errorMessage = "Save failed: ${e.message}")
                }
            } finally {
                tempFile?.delete()
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
                    val pdfBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Failed to read PDF bytes.")
                    pdfEngine.openDocument(pfd, pdfBytes)
                    val pageCount = pdfEngine.getPageCount()
                    if (pageCount <= 0) {
                        throw IllegalStateException("This PDF does not contain any readable pages.")
                    }
                    
                    // Extract document title from URI
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    var title = "Document"
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (displayNameIndex != -1) {
                                title = it.getString(displayNameIndex)
                            }
                        }
                    }

                    val previousDocument = libraryRepository.getRecentDocuments()
                        .firstOrNull { it.uri == uri.toString() }
                    val initialPage = previousDocument?.lastPage
                        ?.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                        ?: 0
                    libraryRepository.recordDocument(
                        RecentDocument(
                            uri = uri.toString(),
                            title = title,
                            pageCount = pageCount,
                            lastPage = initialPage,
                            lastOpenedAt = System.currentTimeMillis(),
                            bookmarkedPages = previousDocument?.bookmarkedPages.orEmpty()
                        )
                    )
                    val recentDocuments = libraryRepository.getRecentDocuments()

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isPdfLoaded = true,
                            pageCount = pageCount,
                            currentPageIndex = initialPage,
                            openedUri = uri,
                            documentTitle = title,
                            recentDocuments = recentDocuments,
                            bookmarkedPages = previousDocument?.bookmarkedPages.orEmpty(),
                            activeTool = AnnotationTool.None,
                            isSavingAnnotations = false,
                            strokesByPage = emptyMap(),
                            highlightsByPage = emptyMap(),
                            embeddedHighlightsByPage = emptyMap(),
                            deletedEmbeddedHighlightIdsByPage = emptyMap(),
                            selectedHighlight = null,
                            textBoxesByPage = emptyMap(),
                            textAnnotationsByPage = emptyMap(),
                            ttsState = TtsState.Idle
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(isLoading = false, errorMessage = "Failed to open file descriptor.") 
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                pdfEngine.closeDocument()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPdfLoaded = false,
                        errorMessage = e.message ?: "Unknown error opening PDF"
                    )
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

    private fun extractPageText(pageIndex: Int, onExtracted: (List<PdfTextBox>) -> Unit) {
        val cached = _state.value.textBoxesByPage[pageIndex]
        if (cached != null) {
            onExtracted(cached)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val boxes = try {
                pdfEngine.getTextBoxes(pageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

            _state.update { state ->
                state.copy(textBoxesByPage = state.textBoxesByPage + (pageIndex to boxes))
            }

            withContext(Dispatchers.Main) {
                onExtracted(boxes)
            }
        }
    }

    private fun extractEmbeddedHighlights(
        pageIndex: Int,
        onLoaded: (List<EmbeddedTextHighlight>) -> Unit
    ) {
        val cached = _state.value.embeddedHighlightsByPage[pageIndex]
        if (cached != null) {
            onLoaded(cached)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val highlights = runCatching { pdfEngine.getEmbeddedHighlights(pageIndex) }
                .getOrElse { emptyList() }
            _state.update { state ->
                state.copy(embeddedHighlightsByPage = state.embeddedHighlightsByPage + (pageIndex to highlights))
            }
            withContext(Dispatchers.Main) { onLoaded(highlights) }
        }
    }

    private fun closePdf() {
        val currentState = _state.value
        val uri = currentState.openedUri?.toString()
        val pageIndex = currentState.currentPageIndex
        progressSaveJob?.cancel()
        if (uri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                libraryRepository.updateProgress(uri, pageIndex)
                val recentDocuments = libraryRepository.getRecentDocuments()
                _state.update { it.copy(recentDocuments = recentDocuments) }
            }
        }
        ttsManager.stop()
        pdfEngine.closeDocument()
        _state.update {
            it.copy(
                isLoading = false,
                isSyncing = false,
                isSavingAnnotations = false,
                isPdfLoaded = false,
                pageCount = 0,
                currentPageIndex = 0,
                openedUri = null,
                documentTitle = null,
                errorMessage = null,
                bookmarkedPages = emptySet(),
                activeTool = AnnotationTool.None,
                strokesByPage = emptyMap(),
                highlightsByPage = emptyMap(),
                embeddedHighlightsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                selectedHighlight = null,
                textBoxesByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                selectedTextPositionByPage = emptyMap(),
                ttsState = TtsState.Idle
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfEngine.closeDocument()
        ttsManager.shutdown()
    }

    private companion object {
        const val PROGRESS_SAVE_DEBOUNCE_MS = 500L
        const val PREFERENCES_SAVE_DEBOUNCE_MS = 250L
    }
}

private fun androidx.compose.ui.geometry.Rect.inflate(amount: Float): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
        left = left - amount,
        top = top - amount,
        right = right + amount,
        bottom = bottom + amount
    )
}
