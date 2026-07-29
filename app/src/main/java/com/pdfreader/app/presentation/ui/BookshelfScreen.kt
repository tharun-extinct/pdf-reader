package com.pdfreader.app.presentation.ui

import android.net.Uri
import androidx.compose.animation.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pdfreader.app.domain.model.RecentDocument
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.theme.DisplayTitleStyle
import com.pdfreader.app.presentation.theme.HeadlineLgMobileStyle
import com.pdfreader.app.presentation.theme.LabelCapsStyle
import com.pdfreader.app.presentation.theme.NoxReaderTheme
import com.pdfreader.app.presentation.theme.UiMainStyle
import com.pdfreader.app.presentation.theme.UiSmStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: PdfReaderViewModel,
    navController: NavController,
    onOpenFilePicker: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val spacing = NoxReaderTheme.spacing

    LaunchedEffect(state.isPdfLoaded) {
        if (state.isPdfLoaded) {
            navController.navigate("reader") {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = "NoxReader",
                            style = DisplayTitleStyle.copy(fontSize = 24.sp, lineHeight = 28.sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your quiet reading space",
                            style = UiSmStyle.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Open settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.isLoading && state.recentDocuments.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onOpenFilePicker,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null
                        )
                    },
                    text = { Text("Open PDF") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                state.isLibraryLoading -> LibraryLoadingState()
                state.recentDocuments.isEmpty() -> EmptyLibrary(
                    errorMessage = state.errorMessage,
                    onOpenFilePicker = onOpenFilePicker,
                    onDismissError = {
                        viewModel.processIntent(PdfReaderIntent.DismissError)
                    }
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(
                            start = spacing.marginMobile,
                            top = 12.dp,
                            end = spacing.marginMobile,
                            bottom = 112.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.isLoading) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }
                        }

                        state.errorMessage?.let { message ->
                            item {
                                ErrorBanner(
                                    message = message,
                                    onDismiss = {
                                        viewModel.processIntent(PdfReaderIntent.DismissError)
                                    }
                                )
                            }
                        }

                        item {
                            ContinueReadingCard(
                                document = state.recentDocuments.first(),
                                onClick = {
                                    viewModel.processIntent(
                                        PdfReaderIntent.OpenPdf(
                                            Uri.parse(state.recentDocuments.first().uri)
                                        )
                                    )
                                }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT DOCUMENTS",
                                    style = LabelCapsStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${state.recentDocuments.size} saved",
                                    style = UiSmStyle.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        items(
                            items = state.recentDocuments,
                            key = { it.uri }
                        ) { document ->
                            RecentDocumentRow(
                                document = document,
                                onClick = {
                                    viewModel.processIntent(
                                        PdfReaderIntent.OpenPdf(Uri.parse(document.uri))
                                    )
                                }
                            )
                        }

                        item {
                            StorageNote()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Preparing your library",
            style = UiSmStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLibrary(
    errorMessage: String?,
    onOpenFilePicker: () -> Unit,
    onDismissError: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        errorMessage?.let {
            item {
                ErrorBanner(message = it, onDismiss = onDismissError)
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "A focused place for every page.",
                        style = HeadlineLgMobileStyle.copy(fontSize = 28.sp, lineHeight = 34.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Open a PDF from your device or cloud provider. NoxReader remembers your place without moving the original file.",
                        style = UiMainStyle.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onOpenFilePicker,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Choose a PDF")
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureRow(
                    icon = Icons.Outlined.Headphones,
                    title = "Read aloud",
                    body = "Listen with synchronized page highlighting."
                )
                FeatureRow(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = "Keep your place",
                    body = "Reading progress and bookmarks stay on this device."
                )
                FeatureRow(
                    icon = Icons.Outlined.CloudDone,
                    title = "Cloud-friendly",
                    body = "Open files through Android’s secure document picker."
                )
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    document: RecentDocument,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = document.progress.coerceIn(0f, 1f),
        label = "Reading progress"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "CONTINUE READING",
                    style = LabelCapsStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = document.title,
                    style = HeadlineLgMobileStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = pageLabel(document),
                    style = UiSmStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                )
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                )
            }
        }
    }
}

@Composable
private fun RecentDocumentRow(
    document: RecentDocument,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 58.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = UiMainStyle.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = pageLabel(document),
                    style = UiSmStyle.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (document.bookmarkedPages.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${document.bookmarkedPages.size} bookmark${if (document.bookmarkedPages.size == 1) "" else "s"}",
                        style = UiSmStyle.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = UiMainStyle.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = UiSmStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = UiSmStyle,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            onDismiss?.let {
                TextButton(
                    onClick = it,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun StorageNote() {
    Text(
        text = "Recent history is stored only on this device. Your PDFs remain in their original location.",
        style = UiSmStyle.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 18.dp)
    )
}

private fun pageLabel(document: RecentDocument): String {
    if (document.pageCount <= 0) return "Ready to read"
    return "Page ${(document.lastPage + 1).coerceAtMost(document.pageCount)} of ${document.pageCount}"
}
