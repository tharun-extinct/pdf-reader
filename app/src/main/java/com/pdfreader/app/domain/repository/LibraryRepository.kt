package com.pdfreader.app.domain.repository

import com.pdfreader.app.domain.model.ReaderPreferences
import com.pdfreader.app.domain.model.RecentDocument

/**
 * Persists small, device-local reader metadata. PDF contents remain at their
 * original Storage Access Framework URI and are never copied into this store.
 */
interface LibraryRepository {
    suspend fun getRecentDocuments(): List<RecentDocument>

    suspend fun recordDocument(document: RecentDocument)

    suspend fun updateProgress(uri: String, pageIndex: Int)

    suspend fun toggleBookmark(uri: String, pageIndex: Int): Set<Int>

    suspend fun clearRecentDocuments()

    suspend fun getPreferences(): ReaderPreferences

    suspend fun savePreferences(preferences: ReaderPreferences)
}
