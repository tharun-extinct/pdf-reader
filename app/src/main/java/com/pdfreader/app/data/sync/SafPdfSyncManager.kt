package com.pdfreader.app.data.sync

import android.content.Context
import android.net.Uri
import com.pdfreader.app.domain.repository.PdfSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SafPdfSyncManager(private val context: Context) : PdfSyncManager {

    override suspend fun copyToLocalCache(uri: Uri): File = withContext(Dispatchers.IO) {
        val fileName = "cached_pdf_${System.currentTimeMillis()}.pdf"
        val localFile = File(context.cacheDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(localFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
        
        localFile
    }

    override suspend fun syncBackToSource(uri: Uri, localFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            // "wt" stands for write and truncate, which overwrites the file cleanly.
            // This natively syncs back to cloud providers like Google Drive via SAF.
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                localFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
