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
import com.pdfreader.app.data.pdfium.PdfiumEngine
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.ui.PdfReaderScreen

class MainActivity : ComponentActivity() {

    // Quick ViewModelFactory for manual dependency injection of PdfiumEngine
    private val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PdfReaderViewModel::class.java)) {
                val pdfEngine = PdfiumEngine(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return PdfReaderViewModel(application, pdfEngine) as T
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
            viewModel.processIntent(PdfReaderIntent.OpenPdf(it))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfReaderScreen(
                        viewModel = viewModel,
                        onOpenFilePicker = {
                            // "application/pdf" filters strictly for PDFs
                            openDocumentLauncher.launch(arrayOf("application/pdf"))
                        }
                    )
                }
            }
        }
    }
}
