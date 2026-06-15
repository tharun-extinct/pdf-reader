package com.pdfreader.app.domain.repository

import android.net.Uri

/**
 * Handles syncing PDFs back to their original source, such as Google Drive
 * via the Android Storage Access Framework (SAF).
 */
interface PdfSyncManager {
    /**
     * Reads the content of the given URI into a local temporary file for fast access/editing.
     */
    suspend fun copyToLocalCache(uri: Uri): java.io.File

    /**
     * Syncs a locally edited PDF file back to the source URI (e.g., Google Drive).
     */
    suspend fun syncBackToSource(uri: Uri, localFile: java.io.File): Boolean
}
