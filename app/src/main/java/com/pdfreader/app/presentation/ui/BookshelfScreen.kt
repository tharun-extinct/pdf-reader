package com.pdfreader.app.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                        Text("⚙")
                    }
                    IconButton(onClick = { navController.navigate("reader") }) {
                        Text("📖")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Placeholder static list of "books"
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Sample PDF 1", "Sample PDF 2", "Sample PDF 3")) { title ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { /* In a real app, open the selected PDF */ }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = title, style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = onOpenFilePicker) {
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}
