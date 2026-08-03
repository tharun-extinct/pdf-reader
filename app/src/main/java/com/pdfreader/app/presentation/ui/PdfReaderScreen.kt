package com.pdfreader.app.presentation.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfreader.app.R
import com.pdfreader.app.presentation.mvi.AnnotationTool
import com.pdfreader.app.presentation.mvi.AnnotationSaveMode
import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.PdfTextBox
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderState
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.domain.tts.TtsState
import androidx.compose.ui.text.style.TextOverflow
import com.pdfreader.app.presentation.theme.UiSmStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PdfReaderScreen(
    viewModel: PdfReaderViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilePicker: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = state.currentPageIndex,
        pageCount = { state.pageCount }
    )

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                viewModel.processIntent(PdfReaderIntent.PageChanged(pageIndex))
            }
    }

    LaunchedEffect(state.ttsState, state.pageCount) {
        if (state.activeTool != AnnotationTool.ReadAloud) return@LaunchedEffect
        val completed = state.ttsState as? TtsState.PageCompleted ?: return@LaunchedEffect
        val nextPage = completed.pageIndex + 1
        if (nextPage >= state.pageCount) {
            viewModel.processIntent(PdfReaderIntent.StopTts)
            return@LaunchedEffect
        }

        pagerState.animateScrollToPage(nextPage)
        val cachedBoxes = state.textBoxesByPage[nextPage]
        if (cachedBoxes != null) {
            viewModel.processIntent(PdfReaderIntent.PlayTts(nextPage, cachedBoxes))
        } else {
            viewModel.processIntent(PdfReaderIntent.RequestPageText(nextPage) { boxes ->
                viewModel.processIntent(PdfReaderIntent.PlayTts(nextPage, boxes))
            })
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // ── Minimal Reading Top Bar (Stitch design) ─────────────
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = state.documentTitle?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.document_default_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.pageCount > 0) {
                            Text(
                                text = stringResource(
                                    R.string.page_of_count,
                                    state.currentPageIndex + 1,
                                    state.pageCount
                                ),
                                style = UiSmStyle.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Save annotations button — only shown when a PDF is loaded
                    if (state.isPdfLoaded) {
                        val hasAnnotations = state.strokesByPage.values.any { it.isNotEmpty() } ||
                            state.highlightsByPage.values.any { it.isNotEmpty() } ||
                            state.textAnnotationsByPage.values.any { it.isNotEmpty() } ||
                            state.deletedEmbeddedHighlightIdsByPage.values.any { it.isNotEmpty() }

                        TextButton(
                            onClick = {
                                val mode = if (state.annotationSaveMode == AnnotationSaveMode.Editable) {
                                    AnnotationSaveMode.Flattened
                                } else {
                                    AnnotationSaveMode.Editable
                                }
                                viewModel.processIntent(PdfReaderIntent.SetAnnotationSaveMode(mode))
                            }
                        ) {
                            Text(
                                stringResource(
                                    if (state.annotationSaveMode == AnnotationSaveMode.Editable) {
                                        R.string.annotation_mode_editable
                                    } else {
                                        R.string.annotation_mode_flattened
                                    }
                                )
                            )
                        }

                        IconButton(
                            onClick = { viewModel.processIntent(PdfReaderIntent.SaveAnnotations) },
                            enabled = hasAnnotations && !state.isSavingAnnotations
                        ) {
                            if (state.isSavingAnnotations) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = stringResource(R.string.save_annotations),
                                    tint = if (hasAnnotations)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }

                    val isBookmarked = state.currentPageIndex in state.bookmarkedPages
                    IconButton(
                        onClick = {
                            viewModel.processIntent(PdfReaderIntent.ToggleBookmark)
                        }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = if (isBookmarked) {
                                stringResource(
                                    R.string.remove_page_bookmark,
                                    state.currentPageIndex + 1
                                )
                            } else {
                                stringResource(
                                    R.string.add_page_bookmark,
                                    state.currentPageIndex + 1
                                )
                            },
                            tint = if (isBookmarked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.opening_document),
                            style = UiSmStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.errorMessage != null -> {
                    ReaderErrorState(
                        message = state.errorMessage.orEmpty(),
                        onChooseAnother = onOpenFilePicker,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.isPdfLoaded -> {
                    PdfPager(
                        state = state,
                        pagerState = pagerState,
                        onIntent = viewModel::processIntent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 96.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                }
                else -> {
                    Button(
                        onClick = onOpenFilePicker,
                        modifier = Modifier.align(Alignment.Center),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.open_pdf))
                    }
                }
            }

            // ── Floating Bottom Toolbar (Stitch pill design) ────────
            if (state.isPdfLoaded) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.activeTool == AnnotationTool.ReadAloud) {
                        TtsControlsOverlay(
                            ttsState = state.ttsState,
                            speechRate = state.preferences.speechRate,
                            onPlay = { 
                                // We need to get the text from the current page.
                                val pageIndex = pagerState.currentPage
                                
                                val textBoxes = state.textBoxesByPage[pageIndex]
                                if (textBoxes != null && textBoxes.isNotEmpty()) {
                                    viewModel.processIntent(PdfReaderIntent.PlayTts(pageIndex, textBoxes))
                                } else {
                                    viewModel.processIntent(PdfReaderIntent.RequestPageText(pageIndex) { boxes ->
                                        viewModel.processIntent(PdfReaderIntent.PlayTts(pageIndex, boxes))
                                    })
                                }
                            },
                            onPause = { viewModel.processIntent(PdfReaderIntent.PauseTts) },
                            onResume = { viewModel.processIntent(PdfReaderIntent.ResumeTts) },
                            onPreviousParagraph = {
                                viewModel.processIntent(PdfReaderIntent.PreviousTtsParagraph)
                            },
                            onNextParagraph = {
                                viewModel.processIntent(PdfReaderIntent.NextTtsParagraph)
                            },
                            onSpeechRateSelected = {
                                viewModel.processIntent(PdfReaderIntent.SetSpeechRate(it))
                            },
                            onStop = { viewModel.processIntent(PdfReaderIntent.StopTts) }
                        )
                    }
                    
                    FloatingAnnotationToolbar(
                        state = state,
                        onIntent = viewModel::processIntent
                    )
                }
            }

        }
    }
}

@Composable
private fun ReaderErrorState(
    message: String,
    onChooseAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.open_pdf_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = UiSmStyle,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onChooseAnother) {
                Text(stringResource(R.string.choose_another_pdf))
            }
        }
    }
}

@Composable
fun TtsControlsOverlay(
    ttsState: TtsState,
    speechRate: Float,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPreviousParagraph: () -> Unit,
    onNextParagraph: () -> Unit,
    onSpeechRateSelected: (Float) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSpeedMenuExpanded by remember { mutableStateOf(false) }
    val paragraphIndex = when (ttsState) {
        is TtsState.Playing -> ttsState.paragraphIndex
        is TtsState.Paused -> ttsState.paragraphIndex
        else -> null
    }
    val paragraphCount = when (ttsState) {
        is TtsState.Playing -> ttsState.paragraphCount
        is TtsState.Paused -> ttsState.paragraphCount
        else -> null
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (ttsState) {
                        is TtsState.Playing -> stringResource(R.string.tts_reading)
                        is TtsState.Paused -> stringResource(R.string.tts_paused)
                        is TtsState.PageCompleted -> stringResource(R.string.tts_turning_page)
                        is TtsState.Error -> stringResource(R.string.tts_unavailable)
                        TtsState.Idle -> stringResource(R.string.tts_idle)
                    },
                    style = UiSmStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (paragraphIndex != null && paragraphCount != null && paragraphCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.tts_paragraph_progress,
                            paragraphIndex + 1,
                            paragraphCount
                        ),
                        style = UiSmStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ttsState is TtsState.Playing || ttsState is TtsState.Paused) {
                    IconButton(onClick = onPreviousParagraph) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.tts_previous_paragraph),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                when (ttsState) {
                    is TtsState.Idle, is TtsState.Error -> {
                        IconButton(onClick = onPlay) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.tts_start),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    is TtsState.Playing -> {
                        IconButton(onClick = onPause) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = stringResource(R.string.tts_pause),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    is TtsState.Paused -> {
                        IconButton(onClick = onResume) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.tts_resume),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    is TtsState.PageCompleted -> Unit
                }
                if (ttsState is TtsState.Playing || ttsState is TtsState.Paused) {
                    IconButton(onClick = onNextParagraph) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.tts_next_paragraph),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (ttsState !is TtsState.Idle && ttsState !is TtsState.Error) {
                    IconButton(onClick = onStop) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = stringResource(R.string.tts_stop),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Box {
                    TextButton(onClick = { isSpeedMenuExpanded = true }) {
                        Text(stringResource(R.string.tts_speed_format, speechRate))
                    }
                    DropdownMenu(
                        expanded = isSpeedMenuExpanded,
                        onDismissRequest = { isSpeedMenuExpanded = false }
                    ) {
                        listOf(0.75f, 1f, 1.25f, 1.5f).forEach { rate ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tts_speed_format, rate)) },
                                onClick = {
                                    isSpeedMenuExpanded = false
                                    onSpeechRateSelected(rate)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating pill-shaped annotation toolbar positioned at the bottom of the reader.
 * Matches the Stitch "Reading: Great Expectations" screen design.
 */
@Composable
private fun FloatingAnnotationToolbar(
    state: PdfReaderState,
    onIntent: (PdfReaderIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Contextual palette row (shown when pen/highlighter is active)
        AnimatedVisibility(
            visible = state.activeTool == AnnotationTool.Pen || state.activeTool == AnnotationTool.Highlighter
        ) {
            val colors = if (state.activeTool == AnnotationTool.Pen)
                state.penPalette.colors else state.highlighterPalette.colors
            val selectedIdx = if (state.activeTool == AnnotationTool.Pen)
                state.selectedPenColorIndex else state.selectedHighlighterColorIndex

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEachIndexed { index, color ->
                        val isSelected = index == selectedIdx
                        val colorDescription = stringResource(R.string.color_number, index + 1)
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 28.dp)
                                .background(Color(color), CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (state.activeTool == AnnotationTool.Pen)
                                        onIntent(PdfReaderIntent.SelectPenColor(index))
                                    else
                                        onIntent(PdfReaderIntent.SelectHighlighterColor(index))
                                }
                                .semantics {
                                    contentDescription = colorDescription
                                    selected = isSelected
                                }
                        )
                    }
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    // Close
                    IconButton(
                        onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.None)) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Main floating toolbar pill
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingToolbarIcon(
                    icon = Icons.Outlined.VolumeUp,
                    label = stringResource(R.string.tts_idle),
                    selected = state.activeTool == AnnotationTool.ReadAloud,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.ReadAloud)) }
                )
                ToolbarDivider()
                FloatingToolbarIcon(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.tool_pen),
                    selected = state.activeTool == AnnotationTool.Pen,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Pen)) }
                )
                FloatingToolbarIcon(
                    icon = Icons.Outlined.Highlight,
                    label = stringResource(R.string.tool_highlighter),
                    selected = state.activeTool == AnnotationTool.Highlighter,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Highlighter)) }
                )
                FloatingToolbarIcon(
                    icon = Icons.Outlined.Backspace,
                    label = stringResource(R.string.tool_eraser),
                    selected = state.activeTool == AnnotationTool.Eraser,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Eraser)) }
                )
                FloatingToolbarIcon(
                    icon = Icons.Outlined.TextFields,
                    label = stringResource(R.string.tool_add_text),
                    selected = state.activeTool == AnnotationTool.AddText,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.AddText)) }
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = CircleShape
            )
            .semantics { contentDescription = label }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfPager(
    state: PdfReaderState,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onIntent: (PdfReaderIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use Crossfade to animate page changes smoothly.
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { pageIndex ->
        androidx.compose.animation.Crossfade(targetState = pageIndex) { index ->
            PdfPage(
                pageIndex = index,
                state = state,
                onIntent = onIntent
            )
        }
    }
}

@Composable
fun PdfPage(
    pageIndex: Int,
    state: PdfReaderState,
    onIntent: (PdfReaderIntent) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Scale state for pinch‑to‑zoom. Starts at 1f (no zoom).
    var scale by remember { mutableStateOf(1f) }

    // When renderRevision increments (annotations baked into PDF & doc re-opened),
    // discard the cached bitmap so the next LaunchedEffect issues a fresh render.
    LaunchedEffect(state.renderRevision) {
        if (state.renderRevision > 0) {
            pageBitmap = null
        }
    }

    LaunchedEffect(size, state.renderRevision) {
        if (size.width > 0 && size.height > 0 && pageBitmap == null) {
            onIntent(
                PdfReaderIntent.RequestPageRender(pageIndex, size.width, size.height) { rendered ->
                    pageBitmap = rendered
                }
            )
        }
    }

    val pageStrokes = state.strokesByPage[pageIndex].orEmpty()
    val pageHighlights = state.highlightsByPage[pageIndex].orEmpty()
    val pageTextBoxes = state.textBoxesByPage[pageIndex].orEmpty()
    val pageTextAnnotations = state.textAnnotationsByPage[pageIndex].orEmpty()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                size = coordinates.size
            },
        contentAlignment = Alignment.Center
    ) {
        val pageImage = pageBitmap
        val contentBounds = remember(size, pageImage) {
            if (size.width == 0 || size.height == 0 || pageImage == null) {
                Rect(0f, 0f, 0f, 0f)
            } else {
                calculateFitBounds(size, pageImage.width, pageImage.height)
            }
        }

        if (pageImage != null) {
            LaunchedEffect(pageIndex, state.renderRevision) {
                if (!state.textBoxesByPage.containsKey(pageIndex)) {
                    onIntent(PdfReaderIntent.RequestPageText(pageIndex) { })
                }
                if (!state.embeddedHighlightsByPage.containsKey(pageIndex)) {
                    onIntent(PdfReaderIntent.RequestPageHighlights(pageIndex))
                }
            }

            // Fade‑in the rendered page for a smoother visual experience.
            AnimatedVisibility(
                visible = pageImage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box {
                    Image(
                    bitmap = pageImage.asImageBitmap(),
                    contentDescription = stringResource(
                        R.string.page_content_description,
                        pageIndex + 1
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                // Clamp the scale to a reasonable range.
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                            }
                        },
                    contentScale = ContentScale.Fit
                )

                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                ) {
                pageHighlights.forEach { highlight ->
                    highlight.rects.forEach { rect ->
                        drawRect(
                            color = Color(highlight.color),
                            topLeft = rect.topLeft.toDisplayOffset(contentBounds),
                            size = androidx.compose.ui.geometry.Size(
                                width = rect.width * contentBounds.width,
                                height = rect.height * contentBounds.height
                            )
                        )
                    }
                }

                pageStrokes.forEach { stroke ->
                    if (stroke.points.isEmpty()) {
                        return@forEach
                    }

                    val path = Path().apply {
                        addSmoothStroke(stroke.points) { it.toDisplayOffset(contentBounds) }
                    }

                    drawPath(
                        path = path,
                        color = Color(stroke.color),
                        style = Stroke(
                            width = stroke.normalizedStrokeWidth * contentBounds.width,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
                }
            }
            } // Close AnimatedVisibility

            if (state.activeTool == AnnotationTool.None || state.activeTool == AnnotationTool.ReadAloud) {
                val ttsState = state.ttsState
                val highlightRects = when {
                    state.activeTool == AnnotationTool.ReadAloud && ttsState is TtsState.Playing && ttsState.pageIndex == pageIndex -> ttsState.highlightRects
                    state.activeTool == AnnotationTool.ReadAloud && ttsState is TtsState.Paused && ttsState.pageIndex == pageIndex -> ttsState.highlightRects
                    else -> emptyList()
                }
                SelectableTextLayer(
                    textBoxes = pageTextBoxes,
                    contentBounds = contentBounds,
                    highlightRects = highlightRects,
                    scale = scale
                )
            }

            AnnotationGestureLayer(
                pageIndex = pageIndex,
                state = state,
                contentBounds = contentBounds,
                scale = scale,
                onIntent = onIntent
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            ) {
                val selectedHighlight = state.selectedHighlight?.takeIf { it.pageIndex == pageIndex }
                if (selectedHighlight != null) {
                    SelectedHighlightOverlay(
                        selected = selectedHighlight,
                        contentBounds = contentBounds,
                        containerSize = size,
                        onDelete = { onIntent(PdfReaderIntent.DeleteSelectedHighlight) }
                    )
                }

                pageTextAnnotations.forEach { annotation ->
                val position = annotation.position.toDisplayOffset(contentBounds)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = position.x.roundToInt(),
                                y = position.y.roundToInt()
                            )
                        }
                        .width(180.dp)
                ) {
                    OutlinedTextField(
                        value = annotation.text,
                        onValueChange = {
                            onIntent(PdfReaderIntent.UpdateTextAnnotation(annotation.id, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text(stringResource(R.string.note_label)) }
                    )
                }
            }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun BoxScope.SelectableTextLayer(
    textBoxes: List<PdfTextBox>,
    contentBounds: Rect,
    highlightRects: List<Rect> = emptyList(),
    scale: Float = 1f
) {
    if (textBoxes.isEmpty() || contentBounds.width <= 0f || contentBounds.height <= 0f) {
        return
    }

    val density = LocalDensity.current
    
    // TTS bounds are normalized to the rendered PDF, not to the whole pager item.
    // Use the fitted PDF bounds so highlights stay aligned when the page is letterboxed.
    if (highlightRects.isNotEmpty()) {
        val ttsHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(scaleX = scale, scaleY = scale)
        ) {
            highlightRects.forEach { rect ->
                val displayRect = rect.toDisplayRect(contentBounds)
                drawRect(
                    color = ttsHighlightColor,
                    topLeft = displayRect.topLeft,
                    size = androidx.compose.ui.geometry.Size(
                        width = displayRect.width,
                        height = displayRect.height
                    )
                )
            }
        }
    }

    SelectionContainer(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            textBoxes.forEach { textBox ->
                val displayBounds = textBox.bounds.toDisplayRect(contentBounds)
                Text(
                    text = textBox.text,
                    color = Color.Transparent,
                    fontSize = 8.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = displayBounds.left.roundToInt(),
                                y = displayBounds.top.roundToInt()
                            )
                        }
                        .width(with(density) { maxOf(displayBounds.width, 1f).toDp() })
                        .height(with(density) { maxOf(displayBounds.height, 1f).toDp() })
                )
            }
        }
    }
}

@Composable
private fun BoxScope.AnnotationGestureLayer(
    pageIndex: Int,
    state: PdfReaderState,
    contentBounds: Rect,
    scale: Float,
    onIntent: (PdfReaderIntent) -> Unit
) {
    val activeTool = state.activeTool
    val penColor = state.penPalette.colors.getOrNull(state.selectedPenColorIndex) ?: state.penPalette.colors.first()
    val highlighterColor = state.highlighterPalette.colors.getOrNull(state.selectedHighlighterColorIndex) ?: state.highlighterPalette.colors.first()
    val currentStrokePoints = remember(pageIndex, activeTool) { mutableStateListOf<Offset>() }

    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(
                when (activeTool) {
                    AnnotationTool.Pen, AnnotationTool.Highlighter -> {
                        Modifier.pointerInput(activeTool, contentBounds, penColor, highlighterColor) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    currentStrokePoints.clear()
                                    toNormalizedIfInside(start, contentBounds)?.let { currentStrokePoints.add(it) }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    toNormalizedIfInside(change.position, contentBounds)?.let { currentStrokePoints.add(it) }
                                },
                                onDragEnd = {
                                    if (currentStrokePoints.size >= 2) {
                                        onIntent(
                                            PdfReaderIntent.AddStroke(
                                                FreehandStroke(
                                                    id = System.currentTimeMillis(),
                                                    pageIndex = pageIndex,
                                                    tool = activeTool,
                                                    color = if (activeTool == AnnotationTool.Pen) penColor else highlighterColor,
                                                    normalizedStrokeWidth = (
                                                        if (activeTool == AnnotationTool.Pen) PEN_STROKE_WIDTH_PX
                                                        else HIGHLIGHTER_STROKE_WIDTH_PX
                                                    ) / contentBounds.width.coerceAtLeast(1f),
                                                    points = currentStrokePoints.toList()
                                                )
                                            )
                                        )
                                    }
                                    currentStrokePoints.clear()
                                },
                                onDragCancel = { currentStrokePoints.clear() }
                            )
                        }
                    }
                    AnnotationTool.Eraser -> {
                        Modifier.pointerInput(activeTool, contentBounds) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                toNormalizedIfInside(change.position, contentBounds)?.let { normalized ->
                                    onIntent(PdfReaderIntent.RemoveStrokeAt(pageIndex, normalized))
                                }
                            }
                        }
                    }
                    AnnotationTool.AddText -> {
                        Modifier.pointerInput(activeTool, contentBounds) {
                            detectTapGestures { tap ->
                                toNormalizedIfInside(tap, contentBounds)?.let { normalized ->
                                    onIntent(PdfReaderIntent.AddTextAnnotation(pageIndex, normalized))
                                }
                            }
                        }
                    }
                    AnnotationTool.None -> {
                        Modifier.pointerInput(pageIndex, contentBounds) {
                            detectTapGestures { tap ->
                                toNormalizedIfInside(tap, contentBounds)?.let { normalized ->
                                    onIntent(PdfReaderIntent.SelectHighlightAt(pageIndex, normalized))
                                } ?: onIntent(PdfReaderIntent.ClearHighlightSelection)
                            }
                        }
                    }
                    else -> Modifier
                }
            )

    ) {
        if (currentStrokePoints.isNotEmpty()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val previewPath = Path().apply {
                    addSmoothStroke(currentStrokePoints) { it.toDisplayOffset(contentBounds) }
                }
                val previewColor = if (activeTool == AnnotationTool.Pen) penColor else highlighterColor
                val previewWidth = if (activeTool == AnnotationTool.Pen) {
                    PEN_STROKE_WIDTH_PX
                } else {
                    HIGHLIGHTER_STROKE_WIDTH_PX
                }
                drawPath(
                    path = previewPath,
                    color = Color(previewColor),
                    style = Stroke(
                        width = previewWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/** Builds a midpoint quadratic spline while retaining the captured points as durable geometry. */
private inline fun Path.addSmoothStroke(
    points: List<Offset>,
    transform: (Offset) -> Offset
) {
    if (points.isEmpty()) return
    val first = transform(points.first())
    moveTo(first.x, first.y)
    if (points.size == 1) return
    for (index in 1 until points.lastIndex) {
        val control = transform(points[index])
        val next = transform(points[index + 1])
        quadraticBezierTo(
            control.x,
            control.y,
            (control.x + next.x) / 2f,
            (control.y + next.y) / 2f
        )
    }
    val last = transform(points.last())
    lineTo(last.x, last.y)
}

private const val PEN_STROKE_WIDTH_PX = 6f
private const val HIGHLIGHTER_STROKE_WIDTH_PX = 22f

@Composable
private fun BoxScope.SelectedHighlightOverlay(
    selected: com.pdfreader.app.presentation.mvi.SelectedHighlight,
    contentBounds: Rect,
    containerSize: IntSize,
    onDelete: () -> Unit
) {
    if (selected.rects.isEmpty()) return
    val displayRects = selected.rects.map { it.toDisplayRect(contentBounds) }
    val bounds = Rect(
        left = displayRects.minOf { it.left },
        top = displayRects.minOf { it.top },
        right = displayRects.maxOf { it.right },
        bottom = displayRects.maxOf { it.bottom }
    )
    Canvas(modifier = Modifier.matchParentSize()) {
        drawRect(
            color = Color(0x332196F3),
            topLeft = bounds.topLeft,
            size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height)
        )
        drawRect(
            color = Color(0xFF2196F3),
            topLeft = bounds.topLeft,
            size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
        )
    }
    val menuWidth = 92.dp.value * androidx.compose.ui.platform.LocalDensity.current.density
    val menuX = bounds.left.coerceIn(0f, (containerSize.width - menuWidth).coerceAtLeast(0f))
    val preferredY = bounds.top - 48.dp.value * androidx.compose.ui.platform.LocalDensity.current.density
    val menuY = (if (preferredY >= 0f) preferredY else bounds.bottom + 8.dp.value * androidx.compose.ui.platform.LocalDensity.current.density)
        .coerceIn(0f, (containerSize.height - 44.dp.value * androidx.compose.ui.platform.LocalDensity.current.density).coerceAtLeast(0f))
    Surface(
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.offset { IntOffset(menuX.roundToInt(), menuY.roundToInt()) }
    ) {
        TextButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.delete))
        }
    }
}

private fun calculateFitBounds(containerSize: IntSize, contentWidth: Int, contentHeight: Int): Rect {
    val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat()
    val contentRatio = contentWidth.toFloat() / contentHeight.toFloat()

    return if (contentRatio > containerRatio) {
        val width = containerSize.width.toFloat()
        val height = width / contentRatio
        val top = (containerSize.height - height) / 2f
        Rect(0f, top, width, top + height)
    } else {
        val height = containerSize.height.toFloat()
        val width = height * contentRatio
        val left = (containerSize.width - width) / 2f
        Rect(left, 0f, left + width, height)
    }
}

private fun Offset.toDisplayOffset(bounds: Rect): Offset {
    return Offset(
        x = bounds.left + (x * bounds.width),
        y = bounds.top + (y * bounds.height)
    )
}

private fun Rect.toDisplayRect(bounds: Rect): Rect {
    val topLeft = topLeft.toDisplayOffset(bounds)
    val bottomRight = bottomRight.toDisplayOffset(bounds)
    return Rect(
        left = topLeft.x,
        top = topLeft.y,
        right = bottomRight.x,
        bottom = bottomRight.y
    )
}

private fun toNormalizedIfInside(position: Offset, bounds: Rect): Offset? {
    if (!bounds.contains(position)) {
        return null
    }

    val width = bounds.width.takeIf { it > 0f } ?: return null
    val height = bounds.height.takeIf { it > 0f } ?: return null

    return Offset(
        x = (position.x - bounds.left) / width,
        y = (position.y - bounds.top) / height
    )
}
