package com.pdfreader.app.presentation.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfreader.app.presentation.mvi.AnnotationTool
import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.PdfTextBox
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderState
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.pdfreader.app.domain.tts.TtsState
import androidx.compose.ui.text.style.TextOverflow
import com.pdfreader.app.presentation.mvi.formatHexColor
import com.pdfreader.app.presentation.mvi.parseHexColor
import com.pdfreader.app.presentation.theme.UiSmStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: PdfReaderViewModel,
    onOpenFilePicker: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // ── Minimal Reading Top Bar (Stitch design) ─────────────
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = { viewModel.processIntent(PdfReaderIntent.ClosePdf) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = state.documentTitle ?: "Document",
                            style = UiSmStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* bookmark */ }) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = "Loading…",
                            style = UiSmStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.errorMessage != null -> {
                    Text(
                        text = "Error: ${state.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.isPdfLoaded -> {
                    PdfPager(
                        state = state,
                        onIntent = viewModel::processIntent
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
                        Text("Open PDF")
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
                            onPlay = { 
                                // We need to get the text from the current page.
                                // For simplicity, we'll just request it if not available.
                                // A more robust solution would track the current visible page.
                                val pageIndex = 0 // TODO: Get actual visible page
                                val textBoxes = state.textBoxesByPage[pageIndex]
                                if (textBoxes != null && textBoxes.isNotEmpty()) {
                                    val text = textBoxes.joinToString(" ") { it.text }
                                    viewModel.processIntent(PdfReaderIntent.PlayTts(text))
                                } else {
                                    viewModel.processIntent(PdfReaderIntent.RequestPageText(pageIndex) { boxes ->
                                        val text = boxes.joinToString(" ") { it.text }
                                        viewModel.processIntent(PdfReaderIntent.PlayTts(text))
                                    })
                                }
                            },
                            onPause = { viewModel.processIntent(PdfReaderIntent.PauseTts) },
                            onResume = { viewModel.processIntent(PdfReaderIntent.ResumeTts) },
                            onStop = { viewModel.processIntent(PdfReaderIntent.StopTts) }
                        )
                    }
                    
                    FloatingAnnotationToolbar(
                        state = state,
                        onIntent = viewModel::processIntent
                    )
                }
            }

            if (state.isAnnotationSettingsOpen) {
                AnnotationSettingsDialog(
                    state = state,
                    onDismiss = { viewModel.processIntent(PdfReaderIntent.ToggleAnnotationSettings) },
                    onSavePenColors = { viewModel.processIntent(PdfReaderIntent.SavePenColors(it)) },
                    onSaveHighlighterColors = { viewModel.processIntent(PdfReaderIntent.SaveHighlighterColors(it)) }
                )
            }
        }
    }
}

@Composable
fun TtsControlsOverlay(
    ttsState: TtsState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (ttsState) {
                is TtsState.Idle, is TtsState.Error -> {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                }
                is TtsState.Playing -> {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                    }
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
                is TtsState.Paused -> {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                    }
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
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
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    label = "Read aloud",
                    selected = state.activeTool == AnnotationTool.ReadAloud,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.ReadAloud)) }
                )
                ToolbarDivider()
                FloatingToolbarIcon(
                    icon = Icons.Outlined.Draw,
                    label = "Pen",
                    selected = state.activeTool == AnnotationTool.Pen,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Pen)) }
                )
                FloatingToolbarIcon(
                    icon = Icons.Outlined.FormatColorFill,
                    label = "Highlighter",
                    selected = state.activeTool == AnnotationTool.Highlighter,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Highlighter)) }
                )
                FloatingToolbarIcon(
                    icon = Icons.Outlined.Delete,
                    label = "Eraser",
                    selected = state.activeTool == AnnotationTool.Eraser,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Eraser)) }
                )
                FloatingToolbarIcon(
                    icon = Icons.Outlined.TextFields,
                    label = "Add text",
                    selected = state.activeTool == AnnotationTool.AddText,
                    onClick = { onIntent(PdfReaderIntent.SelectTool(AnnotationTool.AddText)) }
                )
                ToolbarDivider()
                FloatingToolbarIcon(
                    icon = Icons.Outlined.BookmarkBorder,
                    label = "Bookmark",
                    selected = false,
                    onClick = { /* bookmark page */ }
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
            contentDescription = label,
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
    onIntent: (PdfReaderIntent) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { state.pageCount })

    // Use Crossfade to animate page changes smoothly.
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
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

    LaunchedEffect(size) {
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
            LaunchedEffect(pageIndex) {
                if (!state.textBoxesByPage.containsKey(pageIndex)) {
                    onIntent(PdfReaderIntent.RequestPageText(pageIndex) { })
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
                    contentDescription = "Page $pageIndex",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                // Clamp the scale to a reasonable range.
                                val newScale = (scale * zoom).coerceIn(0.5f, 5f)
                                scale = newScale
                            }
                        },
                    contentScale = ContentScale.Fit
                )

                Canvas(modifier = Modifier.matchParentSize()) {
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
                        stroke.points.forEachIndexed { index, point ->
                            val mapped = point.toDisplayOffset(contentBounds)
                            if (index == 0) {
                                moveTo(mapped.x, mapped.y)
                            } else {
                                lineTo(mapped.x, mapped.y)
                            }
                        }
                    }

                    drawPath(
                        path = path,
                        color = Color(stroke.color),
                        style = Stroke(
                            width = stroke.strokeWidth,
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
                SelectableTextLayer(
                    textBoxes = pageTextBoxes,
                    contentBounds = contentBounds,
                    highlightedParagraphIndex = if (state.activeTool == AnnotationTool.ReadAloud && ttsState is TtsState.Playing) ttsState.paragraphIndex else null
                )
            }

            AnnotationGestureLayer(
                pageIndex = pageIndex,
                state = state,
                contentBounds = contentBounds,
                textBoxes = pageTextBoxes,
                onIntent = onIntent
            )

            pageTextAnnotations.forEach { annotation ->
                val position = annotation.position.toDisplayOffset(contentBounds)
                Surface(
                    color = Color(0xF0FFFFFF),
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
                        label = { Text("Text") }
                    )
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
    highlightedParagraphIndex: Int? = null
) {
    if (textBoxes.isEmpty() || contentBounds.width <= 0f || contentBounds.height <= 0f) {
        return
    }

    val density = LocalDensity.current
    
    // Draw highlights for TTS
    if (highlightedParagraphIndex != null) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Group text boxes into paragraphs (simplified heuristic based on vertical spacing)
            val paragraphs = mutableListOf<List<PdfTextBox>>()
            var currentParagraph = mutableListOf<PdfTextBox>()
            
            textBoxes.sortedBy { it.bounds.top }.forEach { box ->
                if (currentParagraph.isEmpty()) {
                    currentParagraph.add(box)
                } else {
                    val lastBox = currentParagraph.last()
                    // If vertical gap is large, start new paragraph
                    if (box.bounds.top - lastBox.bounds.bottom > 0.02f) {
                        paragraphs.add(currentParagraph)
                        currentParagraph = mutableListOf(box)
                    } else {
                        currentParagraph.add(box)
                    }
                }
            }
            if (currentParagraph.isNotEmpty()) {
                paragraphs.add(currentParagraph)
            }

            if (highlightedParagraphIndex < paragraphs.size) {
                val paragraphBoxes = paragraphs[highlightedParagraphIndex]
                paragraphBoxes.forEach { box ->
                    val rect = box.bounds
                    drawRect(
                        color = Color.Yellow.copy(alpha = 0.3f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = rect.left * contentBounds.width,
                            y = rect.top * contentBounds.height
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            width = rect.width * contentBounds.width,
                            height = rect.height * contentBounds.height
                        )
                    )
                }
            }
        }
    }

    SelectionContainer(modifier = Modifier.matchParentSize()) {
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
    textBoxes: List<PdfTextBox>,
    onIntent: (PdfReaderIntent) -> Unit
) {
    val activeTool = state.activeTool
    val penColor = state.penPalette.colors.getOrNull(state.selectedPenColorIndex) ?: state.penPalette.colors.first()
    val highlighterColor = state.highlighterPalette.colors.getOrNull(state.selectedHighlighterColorIndex) ?: state.highlighterPalette.colors.first()
    var currentStrokePoints = remember { mutableStateListOf<Offset>() }
    var dragStart by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                when (activeTool) {
                    AnnotationTool.Pen, AnnotationTool.Highlighter -> {
                        Modifier.pointerInput(activeTool, contentBounds, textBoxes) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    currentStrokePoints = mutableStateListOf()
                                    dragStart = toNormalizedIfInside(start, contentBounds)
                                    toNormalizedIfInside(start, contentBounds)?.let { currentStrokePoints.add(it) }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    toNormalizedIfInside(change.position, contentBounds)?.let { currentStrokePoints.add(it) }
                                },
                                onDragEnd = {
                                    val start = dragStart
                                    val end = currentStrokePoints.lastOrNull()
                                    val highlightedText = if (activeTool == AnnotationTool.Highlighter && start != null && end != null) {
                                        val selectionRect = normalizedSelectionRect(start, end).inflate(0.006f)
                                        val selectedRects = textBoxes
                                            .filter { it.bounds.intersects(selectionRect) }
                                            .map { it.bounds }
                                        if (selectedRects.isNotEmpty()) {
                                            onIntent(
                                                PdfReaderIntent.AddTextHighlight(
                                                    TextHighlight(
                                                        id = System.currentTimeMillis(),
                                                        pageIndex = pageIndex,
                                                        color = highlighterColor,
                                                        rects = selectedRects
                                                    )
                                                )
                                            )
                                            true
                                        } else {
                                            false
                                        }
                                    } else {
                                        false
                                    }

                                    if (!highlightedText && currentStrokePoints.size >= 2) {
                                        onIntent(
                                            PdfReaderIntent.AddStroke(
                                                FreehandStroke(
                                                    id = System.currentTimeMillis(),
                                                    pageIndex = pageIndex,
                                                    tool = activeTool,
                                                    color = if (activeTool == AnnotationTool.Pen) penColor else highlighterColor,
                                                    strokeWidth = if (activeTool == AnnotationTool.Pen) 6f else 22f,
                                                    points = currentStrokePoints.toList()
                                                )
                                            )
                                        )
                                    }
                                    currentStrokePoints = mutableStateListOf()
                                    dragStart = null
                                }
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
                    else -> Modifier
                }
            )
    )
}

@Composable
private fun AnnotationSettingsDialog(
    state: PdfReaderState,
    onDismiss: () -> Unit,
    onSavePenColors: (List<Long>) -> Unit,
    onSaveHighlighterColors: (List<Long>) -> Unit
) {
    val penInputs = remember(state.penPalette.colors) {
        mutableStateListOf<String>().apply {
            addAll(state.penPalette.colors.map { formatHexColor(it) })
        }
    }
    val highlighterInputs = remember(state.highlighterPalette.colors) {
        mutableStateListOf<String>().apply {
            addAll(state.highlighterPalette.colors.map { formatHexColor(it) })
        }
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Annotation colors", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Use #RRGGBB or #AARRGGBB.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("×", style = MaterialTheme.typography.titleLarge)
                    }
                }

                ColorPaletteEditor(
                    label = "Pen",
                    values = penInputs
                )

                ColorPaletteEditor(
                    label = "Highlighter",
                    values = highlighterInputs
                )

                validationError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val penColors = penInputs.mapNotNull { parseHexColor(it) }
                        val highlighterColors = highlighterInputs.mapNotNull { parseHexColor(it) }
                        if (penColors.size != 4 || highlighterColors.size != 4) {
                            validationError = "All eight colors must be valid hex values."
                            return@Button
                        }

                        onSavePenColors(penColors)
                        onSaveHighlighterColors(highlighterColors)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save colors")
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteEditor(
    label: String,
    values: MutableList<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        values.forEachIndexed { index, value ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(parseHexColor(value) ?: 0x00000000), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { values[index] = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Color ${index + 1}") }
                )
            }
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

private fun normalizedSelectionRect(start: Offset, end: Offset): Rect {
    return Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y)
    )
}

private fun Rect.inflate(amount: Float): Rect {
    return Rect(
        left = (left - amount).coerceIn(0f, 1f),
        top = (top - amount).coerceIn(0f, 1f),
        right = (right + amount).coerceIn(0f, 1f),
        bottom = (bottom + amount).coerceIn(0f, 1f)
    )
}

private fun Rect.intersects(other: Rect): Boolean {
    return left < other.right &&
        right > other.left &&
        top < other.bottom &&
        bottom > other.top
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
