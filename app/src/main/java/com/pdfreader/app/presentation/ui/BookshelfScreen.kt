package com.pdfreader.app.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.theme.NoxReaderTheme
import com.pdfreader.app.presentation.theme.DisplayTitleStyle
import com.pdfreader.app.presentation.theme.HeadlineLgMobileStyle
import com.pdfreader.app.presentation.theme.LabelCapsStyle
import com.pdfreader.app.presentation.theme.UiMainStyle
import com.pdfreader.app.presentation.theme.UiSmStyle
import com.pdfreader.app.presentation.theme.SourceSerif4FontFamily
import com.pdfreader.app.presentation.theme.NoxReaderPrimaryFixed
import com.pdfreader.app.presentation.theme.NoxReaderOnPrimaryFixed
import com.pdfreader.app.presentation.theme.NoxReaderTertiaryFixed
import com.pdfreader.app.presentation.theme.NoxReaderTertiaryFixedDim
import com.pdfreader.app.presentation.theme.NoxReaderPrimaryContainer
import com.pdfreader.app.presentation.theme.NoxReaderOnPrimaryContainer

/**
 * NoxReader Library screen — the app's main landing screen.
 *
 * Design sourced from Stitch "Cloud PDF" project, screen "NoxReader - My Library".
 * Features:
 * - Mobile top bar with hamburger, "NoxReader" title, settings gear
 * - "Continue Reading" hero card
 * - "My Collection" horizontally scrollable book cards
 * - "Annotations & Notes" recent activity list
 * - Bottom navigation bar (All Books, Recent, Annotations, Collections)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: PdfReaderViewModel,
    navController: NavController,
    onOpenFilePicker: () -> Unit
) {
    val spacing = NoxReaderTheme.spacing
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // ── Mobile Top App Bar ──────────────────────────────────
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = { /* drawer toggle */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                title = {
                    Text(
                        text = "NoxReader",
                        style = DisplayTitleStyle.copy(fontSize = 24.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            // ── Bottom Navigation Bar ───────────────────────────────
            NoxReaderBottomNavBar(
                selectedIndex = selectedNavIndex,
                onItemSelected = { selectedNavIndex = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenFilePicker,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Book"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = spacing.gutter)
        ) {
            // ── Continue Reading Hero ───────────────────────────────
            item {
                SectionHeader(
                    title = "CONTINUE READING",
                    modifier = Modifier.padding(horizontal = spacing.marginMobile)
                )
                Spacer(Modifier.height(12.dp))
                ContinueReadingCard(
                    modifier = Modifier.padding(horizontal = spacing.marginMobile),
                    onTap = { navController.navigate("reader") }
                )
            }

            // ── My Collection ───────────────────────────────────────
            item {
                Spacer(Modifier.height(spacing.gutter))
                SectionHeader(
                    title = "MY COLLECTION",
                    modifier = Modifier.padding(horizontal = spacing.marginMobile)
                )
                Spacer(Modifier.height(12.dp))
                BookCollectionRow(
                    onBookTap = { navController.navigate("reader") }
                )
            }

            // ── Annotations & Notes ─────────────────────────────────
            item {
                Spacer(Modifier.height(spacing.gutter))
                SectionHeader(
                    title = "ANNOTATIONS & NOTES",
                    modifier = Modifier.padding(horizontal = spacing.marginMobile)
                )
                Spacer(Modifier.height(12.dp))
                AnnotationsList(
                    modifier = Modifier.padding(horizontal = spacing.marginMobile)
                )
            }
        }
    }
}

// ── Section Header ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = LabelCapsStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

// ── Continue Reading Card ───────────────────────────────────────────────

@Composable
private fun ContinueReadingCard(modifier: Modifier = Modifier, onTap: () -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover placeholder
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 86.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NoxReaderPrimaryContainer,
                                NoxReaderPrimaryFixed
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Great Expectations",
                    style = HeadlineLgMobileStyle.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Charles Dickens",
                    style = UiSmStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { 0.42f },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = "42%",
                        style = UiSmStyle.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Chapter 4: The Digital Sanctuary",
                    style = UiSmStyle.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ── Book Collection Row ─────────────────────────────────────────────────

private data class BookItem(
    val title: String,
    val author: String,
    val progress: Float,
    val gradientColors: List<Color>
)

@Composable
private fun BookCollectionRow(onBookTap: () -> Unit) {
    val spacing = NoxReaderTheme.spacing
    val books = remember {
        listOf(
            BookItem("Pride & Prejudice", "Jane Austen", 0.78f,
                listOf(Color(0xFF3D2907), Color(0xFFAE8F64))),
            BookItem("1984", "George Orwell", 0.15f,
                listOf(Color(0xFF1A2E44), Color(0xFF4C6078))),
            BookItem("The Great Gatsby", "F. Scott Fitzgerald", 0.55f,
                listOf(Color(0xFF35485F), Color(0xFFB4C8E4))),
            BookItem("Brave New World", "Aldous Huxley", 0.0f,
                listOf(Color(0xFF5A431F), Color(0xFFE3C193))),
            BookItem("To Kill a Mockingbird", "Harper Lee", 0.33f,
                listOf(Color(0xFF03192E), Color(0xFF8296B0)))
        )
    }

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.marginMobile),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        books.forEach { book ->
            BookCard(book = book, onClick = onBookTap)
        }
    }
}

@Composable
private fun BookCard(book: BookItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        // Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(book.gradientColors)),
            contentAlignment = Alignment.BottomStart
        ) {
            // Subtle book icon watermark
            Icon(
                imageVector = Icons.Outlined.Book,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
            // Progress indicator at bottom
            if (book.progress > 0f) {
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.title,
            style = UiSmStyle.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = book.author,
            style = UiSmStyle.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Annotations List ────────────────────────────────────────────────────

@Composable
private fun AnnotationsList(modifier: Modifier = Modifier) {
    val annotations = remember {
        listOf(
            Triple("\"The margin is where the reader converses with the author.\"", "Great Expectations · Ch. 4", "Nov 12"),
            Triple("Key theme: invisible design philosophy", "Great Expectations · Ch. 3", "Nov 10"),
            Triple("Architecture of focus — cognitive necessity", "Great Expectations · Ch. 4", "Nov 12")
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        annotations.forEach { (text, source, date) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Annotation marker
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                NoxReaderTertiaryFixed.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = text,
                            style = UiSmStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = source,
                                style = UiSmStyle.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = date,
                                style = UiSmStyle.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Bottom Navigation Bar ───────────────────────────────────────────────

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
private fun NoxReaderBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = remember {
        listOf(
            NavItem("All Books", Icons.Outlined.Book, Icons.Filled.Book),
            NavItem("Recent", Icons.Outlined.History, Icons.Outlined.History),
            NavItem("Annotations", Icons.Outlined.EditNote, Icons.Outlined.EditNote),
            NavItem("Collections", Icons.Outlined.FolderSpecial, Icons.Outlined.FolderSpecial)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (index == selectedIndex) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = UiSmStyle.copy(fontSize = 11.sp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NoxReaderOnPrimaryFixed,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = NoxReaderPrimaryFixed,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
