package com.pdfreader.app.presentation.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pdfreader.app.presentation.mvi.AnnotationTool
import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderState
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.formatHexColor
import com.pdfreader.app.presentation.mvi.parseHexColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: PdfReaderViewModel,
    onOpenFilePicker: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Reader") },
                actions = {
                    if (state.isPdfLoaded) {
                        TextButton(onClick = { viewModel.processIntent(PdfReaderIntent.ToggleAnnotationSettings) }) {
                            Text("Settings")
                        }
                        TextButton(onClick = { viewModel.processIntent(PdfReaderIntent.ClosePdf) }) {
                            Text("Close", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.isPdfLoaded) {
                AnnotationToolbar(
                    state = state,
                    onIntent = viewModel::processIntent
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1C1C1E))
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Open PDF")
                    }
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
private fun AnnotationToolbar(
    state: PdfReaderState,
    onIntent: (PdfReaderIntent) -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state.activeTool) {
                AnnotationTool.Pen -> {
                    ToolPaletteRow(
                        title = "Pen",
                        colors = state.penPalette.colors,
                        selectedIndex = state.selectedPenColorIndex,
                        onColorSelected = { onIntent(PdfReaderIntent.SelectPenColor(it)) }
                    )
                }
                AnnotationTool.Highlighter -> {
                    ToolPaletteRow(
                        title = "Highlighter",
                        colors = state.highlighterPalette.colors,
                        selectedIndex = state.selectedHighlighterColorIndex,
                        onColorSelected = { onIntent(PdfReaderIntent.SelectHighlighterColor(it)) }
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolbarAction("Read aloud", state.activeTool == AnnotationTool.ReadAloud) {
                            onIntent(PdfReaderIntent.SelectTool(AnnotationTool.ReadAloud))
                        }
                        ToolbarAction("Pen", state.activeTool == AnnotationTool.Pen) {
                            onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Pen))
                        }
                        ToolbarAction("Highlighter", state.activeTool == AnnotationTool.Highlighter) {
                            onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Highlighter))
                        }
                        ToolbarAction("Eraser", state.activeTool == AnnotationTool.Eraser) {
                            onIntent(PdfReaderIntent.SelectTool(AnnotationTool.Eraser))
                        }
                        ToolbarAction("Add Text", state.activeTool == AnnotationTool.AddText) {
                            onIntent(PdfReaderIntent.SelectTool(AnnotationTool.AddText))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolPaletteRow(
    title: String,
    colors: List<Long>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        colors.forEachIndexed { index, color ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(color), CircleShape)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(index) }
            )
        }
    }
}

@Composable
private fun ToolbarAction(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfPager(
    state: PdfReaderState,
    onIntent: (PdfReaderIntent) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { state.pageCount })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { pageIndex ->
        PdfPage(
            pageIndex = pageIndex,
            state = state,
            onIntent = onIntent
        )
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
            Image(
                bitmap = pageImage.asImageBitmap(),
                contentDescription = "Page $pageIndex",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Canvas(modifier = Modifier.matchParentSize()) {
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

            AnnotationGestureLayer(
                pageIndex = pageIndex,
                state = state,
                contentBounds = contentBounds,
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
private fun AnnotationGestureLayer(
    pageIndex: Int,
    state: PdfReaderState,
    contentBounds: Rect,
    onIntent: (PdfReaderIntent) -> Unit
) {
    val activeTool = state.activeTool
    val penColor = state.penPalette.colors.getOrNull(state.selectedPenColorIndex) ?: state.penPalette.colors.first()
    val highlighterColor = state.highlighterPalette.colors.getOrNull(state.selectedHighlighterColorIndex) ?: state.highlighterPalette.colors.first()
    val currentStrokePoints = remember { mutableStateListOf<Offset>() }

    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                when (activeTool) {
                    AnnotationTool.Pen, AnnotationTool.Highlighter -> {
                        Modifier.pointerInput(activeTool, contentBounds) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    currentStrokePoints = mutableStateListOf()
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
                                                    strokeWidth = if (activeTool == AnnotationTool.Pen) 6f else 22f,
                                                    points = currentStrokePoints.toList()
                                                )
                                            )
                                        )
                                    }
                                    currentStrokePoints = mutableStateListOf()
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0x80000000)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Annotation colors", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Enter four hex colors for each tool. Use #RRGGBB or #AARRGGBB.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            val penColors = penInputs.mapNotNull { parseHexColor(it) }
                            val highlighterColors = highlighterInputs.mapNotNull { parseHexColor(it) }
                            if (penColors.size != 4 || highlighterColors.size != 4) {
                                validationError = "All eight colors must be valid hex values."
                                return@TextButton
                            }

                            onSavePenColors(penColors)
                            onSaveHighlighterColors(highlighterColors)
                            onDismiss()
                        }) {
                            Text("Save")
                        }
                    }
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
