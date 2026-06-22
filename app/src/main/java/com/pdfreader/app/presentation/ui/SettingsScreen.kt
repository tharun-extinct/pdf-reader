package com.pdfreader.app.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel

/**
 * A dedicated Settings screen (not a dialog) that can be navigated to from the Bookshelf.
 * For now it contains a few placeholder toggles to demonstrate the layout. Real settings
 * would be persisted via DataStore or SharedPreferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PdfReaderViewModel,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
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
            // Dark mode toggle (placeholder)
            val darkModeEnabled = remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("Dark Mode") },
                trailingContent = {
                    Switch(checked = darkModeEnabled.value, onCheckedChange = { darkModeEnabled.value = it })
                }
            )

            // Sync with Google Drive toggle (placeholder)
            val syncEnabled = remember { mutableStateOf(true) }
            ListItem(
                headlineContent = { Text("Sync with Google Drive") },
                trailingContent = {
                    Switch(checked = syncEnabled.value, onCheckedChange = { syncEnabled.value = it })
                }
            )
        }
    }
}
