package com.pdfreader.app.presentation.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderState
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel

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
                        TextButton(onClick = { viewModel.processIntent(PdfReaderIntent.ClosePdf) }) {
                            Text("Close", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.DarkGray)
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
                        pageCount = state.pageCount,
                        onRenderPage = { pageIndex, width, height, callback ->
                            viewModel.processIntent(
                                PdfReaderIntent.RequestPageRender(pageIndex, width, height, callback)
                            )
                        }
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfPager(
    pageCount: Int,
    onRenderPage: (Int, Int, Int, (Bitmap?) -> Unit) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pageCount })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { pageIndex ->
        PdfPage(
            pageIndex = pageIndex,
            onRenderPage = onRenderPage
        )
    }
}

@Composable
fun PdfPage(
    pageIndex: Int,
    onRenderPage: (Int, Int, Int, (Bitmap?) -> Unit) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // When the size is available, request the bitmap
    LaunchedEffect(size) {
        if (size.width > 0 && size.height > 0 && pageBitmap == null) {
            onRenderPage(pageIndex, size.width, size.height) { rendered ->
                pageBitmap = rendered
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                size = coordinates.size
            },
        contentAlignment = Alignment.Center
    ) {
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "Page $pageIndex",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
