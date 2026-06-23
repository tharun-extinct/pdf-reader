package com.pdfreader.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pdfreader.app.data.pdfium.PdfiumEngine
import com.pdfreader.app.data.sync.SafPdfSyncManager
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.ui.PdfReaderScreen
import com.pdfreader.app.presentation.ui.BookshelfScreen
import com.pdfreader.app.presentation.ui.SettingsScreen

import com.pdfreader.app.presentation.theme.LibroTheme

class MainActivity : ComponentActivity() {

    // Quick ViewModelFactory for manual dependency injection
    private val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PdfReaderViewModel::class.java)) {
                val pdfEngine = PdfiumEngine(applicationContext)
                val syncManager = SafPdfSyncManager(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return PdfReaderViewModel(application, pdfEngine, syncManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val viewModel: PdfReaderViewModel by viewModels { viewModelFactory }

    // Launcher to pick PDF files
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent read/write permission to allow background syncing to Google Drive
            try {
                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            
            viewModel.processIntent(PdfReaderIntent.OpenPdf(it))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "bookshelf") {
                        composable("bookshelf") {
                            BookshelfScreen(
                                viewModel = viewModel,
                                navController = navController,
                                onOpenFilePicker = {
                                    openDocumentLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }
                        composable("reader") {
                            PdfReaderScreen(
                                viewModel = viewModel,
                                onOpenFilePicker = {
                                    openDocumentLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}
