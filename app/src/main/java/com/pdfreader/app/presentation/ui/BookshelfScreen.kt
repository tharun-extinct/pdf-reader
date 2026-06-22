package com.pdfreader.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel

/**
 * Simple placeholder for a bookshelf view. In a full implementation this would query a Room
 * database for stored books and display thumbnails. For now we provide a static list with a
 * button to open a PDF via the existing file picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: PdfReaderViewModel,
    navController: NavController,
    onOpenFilePicker: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookshelf") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(painter = painterResource(id = android.R.drawable.ic_menu_preferences), contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenFilePicker) {
                Icon(painter = painterResource(id = android.R.drawable.ic_input_add), contentDescription = "Add Book")
            }
        }
    ) { paddingValues ->
        // Simple placeholder data set
        val books = remember { listOf("Sample PDF 1", "Sample PDF 2", "Sample PDF 3") }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books) { title ->
                Column(
                    modifier = Modifier
                        .clickable { navController.navigate("reader") }
                        .padding(8.dp)
                ) {
                    // Placeholder thumbnail
                    // Placeholder thumbnail box
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFCCCCCC),
                                shape = MaterialTheme.shapes.medium
                            )
                            .size(100.dp)
                    ) {}
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
