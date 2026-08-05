package com.pdfreader.app.presentation.mvi

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfreader.app.R
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
        if (_state.value.isSavingAnnotations && intent.mutatesAnnotations()) return
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
            is PdfReaderIntent.RequestPageInk -> extractEmbeddedInk(intent.pageIndex)
            is PdfReaderIntent.RequestPageTextAnnotations -> extractEmbeddedTextAnnotations(intent.pageIndex)
            is PdfReaderIntent.SelectTool -> selectTool(intent.tool)
            is PdfReaderIntent.SelectPenColor -> selectPenColor(intent.index)
            is PdfReaderIntent.SelectHighlighterColor -> selectHighlighterColor(intent.index)
            is PdfReaderIntent.SavePenColors -> savePenColors(intent.colors)
            is PdfReaderIntent.SaveHighlighterColors -> saveHighlighterColors(intent.colors)
            is PdfReaderIntent.AddStroke -> addStroke(intent.stroke)
            is PdfReaderIntent.AddTextHighlight -> addTextHighlight(intent.highlight)
            is PdfReaderIntent.RemoveStrokeAt -> removeStrokeAt(intent.pageIndex, intent.position)
            is PdfReaderIntent.AddTextAnnotation -> addTextAnnotation(intent.pageIndex, intent.position)
            is PdfReaderIntent.UpdateTextAnnotation -> updateTextAnnotation(intent.annotationId, intent.text)
            is PdfReaderIntent.SelectTextAnnotation -> selectTextAnnotation(intent.annotationId)
            is PdfReaderIntent.ResizeTextAnnotation -> resizeTextAnnotation(
                intent.annotationId,
                intent.handle,
                intent.normalizedDelta
            )
            is PdfReaderIntent.SelectAnnotationAt -> selectAnnotationAt(intent.pageIndex, intent.position)
            is PdfReaderIntent.ClearAnnotationSelection -> _state.update {
                it.copy(
                    selectedHighlight = null,
                    selectedInk = null,
                    selectedTextAnnotationId = null
                )
            }
            is PdfReaderIntent.DeleteSelectedAnnotation -> deleteSelectedAnnotation()
            is PdfReaderIntent.PlayTts -> playTts(intent.pageIndex, intent.textBoxes)
            is PdfReaderIntent.PauseTts -> ttsManager.pause()
            is PdfReaderIntent.ResumeTts -> ttsManager.resume()
            is PdfReaderIntent.PreviousTtsParagraph -> ttsManager.previousParagraph()
            is PdfReaderIntent.NextTtsParagraph -> ttsManager.nextParagraph()
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
        _state.update { state -> state.copy(activeTool = nextTool) }
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

    private fun savePenColors(colors: List<Long>) {
        if (colors.size != 4) return
        updatePreferences { it.copy(penColors = colors) }
        _state.update { state ->
            state.copy(selectedPenColorIndex = state.selectedPenColorIndex.coerceIn(0, colors.lastIndex))
        }
    }

    private fun saveHighlighterColors(colors: List<Long>) {
        if (colors.size != 4) return
        updatePreferences { it.copy(highlighterColors = colors) }
        _state.update { state ->
            state.copy(
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
                highlightsByPage = state.highlightsByPage + (highlight.pageIndex to (pageHighlights + highlight)),
                selectedHighlight = SelectedHighlight(
                    id = highlight.id.toString(),
                    pageIndex = highlight.pageIndex,
                    source = HighlightSource.Session,
                    color = highlight.color,
                    rects = highlight.rects
                ),
                selectedInk = null,
                selectedTextAnnotationId = null
            )
        }
    }

    private fun selectAnnotationAt(pageIndex: Int, position: androidx.compose.ui.geometry.Offset) {
        _state.update { state ->
            val selectedText = TextAnnotationGeometry.select(
                position,
                state.textAnnotationsByPage[pageIndex].orEmpty()
            )
            if (selectedText != null) {
                return@update state.copy(
                    selectedTextAnnotationId = selectedText.id,
                    selectedInk = null,
                    selectedHighlight = null
                )
            }
            val deletedTextIds = state.deletedEmbeddedTextAnnotationIdsByPage[pageIndex].orEmpty()
            val selectedEmbeddedText = TextAnnotationGeometry.selectEmbedded(
                position = position,
                annotations = state.embeddedTextAnnotationsByPage[pageIndex].orEmpty(),
                deletedIds = deletedTextIds
            )
            if (selectedEmbeddedText != null) {
                return@update state.copy(
                    selectedTextAnnotationId = selectedEmbeddedText.id,
                    selectedInk = null,
                    selectedHighlight = null
                )
            }
            val sessionInk = state.strokesByPage[pageIndex].orEmpty().map { stroke ->
                SelectedInk(
                    id = stroke.id.toString(),
                    pageIndex = pageIndex,
                    source = InkSource.Session,
                    color = stroke.color,
                    normalizedStrokeWidth = stroke.normalizedStrokeWidth,
                    paths = listOf(stroke.points)
                )
            }
            val deletedInkIds = state.deletedEmbeddedInkIdsByPage[pageIndex].orEmpty()
            val embeddedInk = state.embeddedInkByPage[pageIndex].orEmpty()
                .filterNot { it.id in deletedInkIds }
                .map { ink ->
                    SelectedInk(
                        id = ink.id,
                        pageIndex = pageIndex,
                        source = InkSource.Embedded,
                        color = ink.color,
                        normalizedStrokeWidth = ink.normalizedStrokeWidth,
                        paths = ink.paths
                    )
                }
            val selectedInk = InkHitTester.select(position, sessionInk + embeddedInk)
            if (selectedInk != null) {
                return@update state.copy(
                    selectedInk = selectedInk,
                    selectedHighlight = null,
                    selectedTextAnnotationId = null
                )
            }
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
            state.copy(
                selectedHighlight = selected,
                selectedInk = null,
                selectedTextAnnotationId = null
            )
        }
    }

    private fun deleteSelectedAnnotation() {
        _state.update { state ->
            val selectedTextId = state.selectedTextAnnotationId
            if (selectedTextId != null) {
                val pendingExists = state.textAnnotationsByPage.values.any { annotations ->
                    annotations.any { it.id == selectedTextId }
                }
                if (pendingExists) {
                    return@update state.copy(
                        textAnnotationsByPage = state.textAnnotationsByPage.mapValues { (_, annotations) ->
                            annotations.filterNot { it.id == selectedTextId }
                        },
                        selectedTextAnnotationId = null
                    )
                }
                val embedded = state.embeddedTextAnnotationsByPage.values
                    .flatten()
                    .firstOrNull { it.id == selectedTextId }
                if (embedded != null) {
                    val deletedIds = state.deletedEmbeddedTextAnnotationIdsByPage[embedded.pageIndex].orEmpty()
                    return@update state.copy(
                        deletedEmbeddedTextAnnotationIdsByPage =
                            state.deletedEmbeddedTextAnnotationIdsByPage +
                                (embedded.pageIndex to (deletedIds + embedded.embeddedId)),
                        selectedTextAnnotationId = null
                    )
                }
            }
            val selectedInk = state.selectedInk
            if (selectedInk != null) {
                return@update when (selectedInk.source) {
                    InkSource.Session -> state.copy(
                        strokesByPage = state.strokesByPage + (
                            selectedInk.pageIndex to state.strokesByPage[selectedInk.pageIndex].orEmpty()
                                .filterNot { it.id.toString() == selectedInk.id }
                            ),
                        selectedInk = null
                    )
                    InkSource.Embedded -> {
                        val existing = state.deletedEmbeddedInkIdsByPage[selectedInk.pageIndex].orEmpty()
                        state.copy(
                            deletedEmbeddedInkIdsByPage = state.deletedEmbeddedInkIdsByPage +
                                (selectedInk.pageIndex to (existing + selectedInk.id)),
                            selectedInk = null
                        )
                    }
                }
            }
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
                bounds = TextAnnotationGeometry.createBounds(position),
                color = state.penPalette.colors[state.selectedPenColorIndex],
                text = ""
            )
            state.copy(
                textAnnotationsByPage = state.textAnnotationsByPage +
                    (pageIndex to (pageAnnotations + annotation)),
                selectedTextAnnotationId = annotation.id,
                selectedHighlight = null,
                selectedInk = null
            )
        }
    }

    private fun updateTextAnnotation(annotationId: Long, text: String) {
        _state.update { state ->
            val pendingId = state.resolvePendingTextAnnotationId(annotationId)
            if (pendingId == null) {
                return@update promoteEmbeddedTextAnnotation(state, annotationId) { annotation ->
                    annotation.copy(text = text)
                } ?: state
            }
            val updatedPages = state.textAnnotationsByPage.mapValues { (_, annotations) ->
                annotations.map { annotation ->
                    if (annotation.id == pendingId) annotation.copy(text = text) else annotation
                }
            }
            state.copy(textAnnotationsByPage = updatedPages)
        }
    }

    private fun selectTextAnnotation(annotationId: Long) {
        _state.update { state ->
            val exists = state.textAnnotationsByPage.values.any { annotations ->
                annotations.any { it.id == annotationId }
            } || state.embeddedTextAnnotationsByPage.values.any { annotations ->
                annotations.any { it.id == annotationId }
            }
            if (!exists) state else state.copy(
                selectedTextAnnotationId = annotationId,
                selectedHighlight = null,
                selectedInk = null
            )
        }
    }

    private fun resizeTextAnnotation(
        annotationId: Long,
        handle: TextAnnotationHandle,
        normalizedDelta: androidx.compose.ui.geometry.Offset
    ) {
        _state.update { state ->
            val pendingId = state.resolvePendingTextAnnotationId(annotationId)
            if (pendingId == null) {
                return@update promoteEmbeddedTextAnnotation(state, annotationId) { annotation ->
                    annotation.copy(
                        bounds = TextAnnotationGeometry.resize(
                            annotation.bounds,
                            handle,
                            normalizedDelta
                        )
                    )
                } ?: state
            }
            val updatedPages = state.textAnnotationsByPage.mapValues { (_, annotations) ->
                annotations.map { annotation ->
                    if (annotation.id == pendingId) {
                        annotation.copy(
                            bounds = TextAnnotationGeometry.resize(
                                annotation.bounds,
                                handle,
                                normalizedDelta
                            )
                        )
                    } else {
                        annotation
                    }
                }
            }
            state.copy(
                textAnnotationsByPage = updatedPages,
                selectedTextAnnotationId = pendingId
            )
        }
    }

    private fun promoteEmbeddedTextAnnotation(
        state: PdfReaderState,
        annotationId: Long,
        transform: (TextAnnotation) -> TextAnnotation
    ): PdfReaderState? {
        val embedded = state.embeddedTextAnnotationsByPage.values
            .flatten()
            .firstOrNull { it.id == annotationId }
            ?: return null
        if (embedded.embeddedId in state.deletedEmbeddedTextAnnotationIdsByPage[embedded.pageIndex].orEmpty()) {
            return null
        }

        val pendingIds = state.textAnnotationsByPage.values.flatten().mapTo(mutableSetOf()) { it.id }
        var replacementId = System.currentTimeMillis().coerceAtLeast(1L)
        while (replacementId in pendingIds) replacementId++
        val replacement = transform(
            TextAnnotation(
                id = replacementId,
                pageIndex = embedded.pageIndex,
                position = embedded.position,
                bounds = TextAnnotationGeometry.createBounds(embedded.position),
                color = embedded.color,
                text = embedded.text,
                sourceEmbeddedAnnotationId = embedded.id
            )
        )
        val pageAnnotations = state.textAnnotationsByPage[embedded.pageIndex].orEmpty()
        val deletedIds = state.deletedEmbeddedTextAnnotationIdsByPage[embedded.pageIndex].orEmpty()
        return state.copy(
            textAnnotationsByPage = state.textAnnotationsByPage +
                (embedded.pageIndex to (pageAnnotations + replacement)),
            deletedEmbeddedTextAnnotationIdsByPage =
                state.deletedEmbeddedTextAnnotationIdsByPage +
                    (embedded.pageIndex to (deletedIds + embedded.embeddedId)),
            selectedTextAnnotationId = replacement.id
        )
    }

    private fun syncPdf(localFile: java.io.File) {
        val uri = _state.value.openedUri ?: return
        _state.update { it.copy(isSyncing = true, errorMessage = null) }
        
        viewModelScope.launch(Dispatchers.IO) {
            val success = syncManager.syncBackToSource(uri, localFile)
            if (!success) {
                _state.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = getApplication<Application>().getString(
                            R.string.error_sync_provider
                        )
                    )
                }
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
        val textReselectionTarget = currentState.selectedTextAnnotationId?.let { selectedId ->
            createTextReselectionTarget(currentState, selectedId)
        }

        // Nothing to save
        val hasAnnotations = currentState.strokesByPage.values.any { it.isNotEmpty() } ||
            currentState.highlightsByPage.values.any { it.isNotEmpty() } ||
            currentState.textAnnotationsByPage.values.any { it.isNotEmpty() } ||
            currentState.deletedEmbeddedHighlightIdsByPage.values.any { it.isNotEmpty() } ||
            currentState.deletedEmbeddedInkIdsByPage.values.any { it.isNotEmpty() } ||
            currentState.deletedEmbeddedTextAnnotationIdsByPage.values.any { it.isNotEmpty() }
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
                    deletedEmbeddedInkIdsByPage = currentState.deletedEmbeddedInkIdsByPage,
                    deletedEmbeddedTextAnnotationIdsByPage =
                        currentState.deletedEmbeddedTextAnnotationIdsByPage,
                    outputFile = outputFile
                )

                // Sync annotated file back to the source URI (Google Drive, local, etc.)
                val success = syncManager.syncBackToSource(uri, outputFile)
                if (!success) {
                    _state.update {
                        it.copy(
                            isSavingAnnotations = false,
                            errorMessage = context.getString(
                                R.string.error_save_annotations_source
                            )
                        )
                    }
                    return@launch
                }

                // Re-open the document so PDFium renders the embedded annotations
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalStateException(
                        context.getString(R.string.error_reopen_after_save)
                    )
                val newBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException(
                        context.getString(R.string.error_reread_after_save)
                    )
                pdfEngine.openDocument(pfd, newBytes)

                val reopenedTextAnnotations = textReselectionTarget?.let { target ->
                    runCatching { pdfEngine.getEmbeddedTextAnnotations(target.pageIndex) }
                        .getOrElse { emptyList() }
                }.orEmpty()
                val reselectedTextAnnotation = textReselectionTarget?.let { target ->
                    findReselectedTextAnnotation(target, reopenedTextAnnotations)
                }

                // Bump renderRevision: PdfPage composables observe this key and will
                // discard their cached bitmaps, triggering a fresh render that shows
                // the now-embedded annotations via PDFium.
                _state.update {
                    it.copy(
                        isSavingAnnotations = false,
                        strokesByPage = emptyMap(),
                        highlightsByPage = emptyMap(),
                        textAnnotationsByPage = emptyMap(),
                        embeddedTextAnnotationsByPage = textReselectionTarget?.let { target ->
                            mapOf(target.pageIndex to reopenedTextAnnotations)
                        }.orEmpty(),
                        deletedEmbeddedTextAnnotationIdsByPage = emptyMap(),
                        embeddedHighlightsByPage = emptyMap(),
                        deletedEmbeddedHighlightIdsByPage = emptyMap(),
                        embeddedInkByPage = emptyMap(),
                        deletedEmbeddedInkIdsByPage = emptyMap(),
                        selectedHighlight = null,
                        selectedInk = null,
                        selectedTextAnnotationId = reselectedTextAnnotation?.id,
                        textBoxesByPage = emptyMap(), // invalidated by document re-open
                        renderRevision = it.renderRevision + 1
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    val context = getApplication<Application>()
                    it.copy(
                        isSavingAnnotations = false,
                        errorMessage = context.getString(
                            R.string.error_save_annotations,
                            e.localizedMessage
                                ?: context.getString(R.string.error_unknown_reason)
                        )
                    )
                }
            } finally {
                tempFile?.delete()
            }
        }
    }

    private fun openPdf(uri: Uri) {
        ttsManager.stop()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                
                if (pfd != null) {
                    val pdfBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException(
                            context.getString(R.string.error_read_pdf_bytes)
                        )
                    pdfEngine.openDocument(pfd, pdfBytes)
                    val pageCount = pdfEngine.getPageCount()
                    if (pageCount <= 0) {
                        throw IllegalStateException(
                            context.getString(R.string.error_no_readable_pages)
                        )
                    }
                    
                    // Extract document title from URI
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    var title = ""
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
                            selectedInk = null,
                            selectedTextAnnotationId = null,
                            textBoxesByPage = emptyMap(),
                            textAnnotationsByPage = emptyMap(),
                            embeddedTextAnnotationsByPage = emptyMap(),
                            deletedEmbeddedTextAnnotationIdsByPage = emptyMap(),
                            ttsState = TtsState.Idle
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = context.getString(
                                R.string.error_open_file_descriptor
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                pdfEngine.closeDocument()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPdfLoaded = false,
                        errorMessage = e.localizedMessage
                            ?: getApplication<Application>().getString(
                                R.string.error_open_pdf_unknown
                            )
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

    private fun extractEmbeddedInk(pageIndex: Int) {
        if (_state.value.embeddedInkByPage.containsKey(pageIndex)) return
        viewModelScope.launch(Dispatchers.IO) {
            val ink = runCatching { pdfEngine.getEmbeddedInk(pageIndex) }.getOrElse { emptyList() }
            _state.update { state ->
                state.copy(embeddedInkByPage = state.embeddedInkByPage + (pageIndex to ink))
            }
        }
    }

    private fun extractEmbeddedTextAnnotations(pageIndex: Int) {
        if (_state.value.embeddedTextAnnotationsByPage.containsKey(pageIndex)) return
        viewModelScope.launch(Dispatchers.IO) {
            val annotations = runCatching { pdfEngine.getEmbeddedTextAnnotations(pageIndex) }
                .getOrElse { emptyList() }
            _state.update { state ->
                state.copy(
                    embeddedTextAnnotationsByPage =
                        state.embeddedTextAnnotationsByPage + (pageIndex to annotations)
                )
            }
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
                embeddedInkByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = emptyMap(),
                selectedHighlight = null,
                selectedInk = null,
                selectedTextAnnotationId = null,
                textBoxesByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                embeddedTextAnnotationsByPage = emptyMap(),
                deletedEmbeddedTextAnnotationIdsByPage = emptyMap(),
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

internal data class TextAnnotationReselectionTarget(
    val pageIndex: Int,
    val position: androidx.compose.ui.geometry.Offset,
    val text: String,
    val expectedSourceAnnotationId: Long?
)

private fun createTextReselectionTarget(
    state: PdfReaderState,
    selectedId: Long
): TextAnnotationReselectionTarget? {
    val pending = state.textAnnotationsByPage.values.flatten().firstOrNull { it.id == selectedId }
    if (pending != null) {
        return TextAnnotationReselectionTarget(
            pageIndex = pending.pageIndex,
            position = pending.position,
            text = pending.text,
            expectedSourceAnnotationId = pending.id
        )
    }
    val embedded = state.embeddedTextAnnotationsByPage.values.flatten().firstOrNull { it.id == selectedId }
        ?: return null
    return TextAnnotationReselectionTarget(
        pageIndex = embedded.pageIndex,
        position = embedded.position,
        text = embedded.text,
        expectedSourceAnnotationId = embedded.sourceAnnotationId
    )
}

internal fun findReselectedTextAnnotation(
    target: TextAnnotationReselectionTarget,
    annotations: List<EmbeddedTextAnnotation>
): EmbeddedTextAnnotation? {
    target.expectedSourceAnnotationId?.let { sourceId ->
        annotations.firstOrNull { it.sourceAnnotationId == sourceId }?.let { return it }
    }
    return annotations
        .asSequence()
        .filter { it.text == target.text }
        .minByOrNull { annotation ->
            hypot(
                annotation.position.x - target.position.x,
                annotation.position.y - target.position.y
            )
        }
        ?.takeIf { annotation ->
            hypot(
                annotation.position.x - target.position.x,
                annotation.position.y - target.position.y
            ) <= TEXT_ANNOTATION_RESELECTION_DISTANCE
        }
}

internal fun PdfReaderState.resolvePendingTextAnnotationId(annotationId: Long): Long? =
    textAnnotationsByPage.values
        .flatten()
        .firstOrNull {
            it.id == annotationId || it.sourceEmbeddedAnnotationId == annotationId
        }
        ?.id

private fun PdfReaderIntent.mutatesAnnotations(): Boolean = when (this) {
    is PdfReaderIntent.AddStroke,
    is PdfReaderIntent.AddTextHighlight,
    is PdfReaderIntent.RemoveStrokeAt,
    is PdfReaderIntent.AddTextAnnotation,
    is PdfReaderIntent.UpdateTextAnnotation,
    is PdfReaderIntent.ResizeTextAnnotation,
    PdfReaderIntent.DeleteSelectedAnnotation,
    PdfReaderIntent.SaveAnnotations -> true
    else -> false
}

private fun androidx.compose.ui.geometry.Rect.inflate(amount: Float): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
        left = left - amount,
        top = top - amount,
        right = right + amount,
        bottom = bottom + amount
    )
}

private const val TEXT_ANNOTATION_RESELECTION_DISTANCE = 0.02f
