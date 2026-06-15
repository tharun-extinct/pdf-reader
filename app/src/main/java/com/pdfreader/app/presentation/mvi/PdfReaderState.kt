package com.pdfreader.app.presentation.mvi

import android.net.Uri

/**
 * Represents the immutable state of the PDF Reader UI.
 */
data class PdfReaderState(
    val isLoading: Boolean = false,
    val isPdfLoaded: Boolean = false,
    val pageCount: Int = 0,
    val openedUri: Uri? = null,
    val errorMessage: String? = null
)
