package com.pdfreader.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import com.pdfreader.app.data.preferences.proto.ReaderDataProto
import com.pdfreader.app.data.preferences.proto.ReaderPreferencesProto
import com.pdfreader.app.data.preferences.proto.RecentDocumentProto
import com.pdfreader.app.data.preferences.proto.ThemeModeProto
import com.pdfreader.app.domain.model.ReaderPreferences
import com.pdfreader.app.domain.model.RecentDocument
import com.pdfreader.app.domain.model.ThemeMode
import com.pdfreader.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Stores small reader metadata in one versioned Proto DataStore. Every mutation
 * uses DataStore's atomic read-transform-write operation.
 */
class ProtoLibraryRepository internal constructor(
    private val dataStore: DataStore<ReaderDataProto>
) : LibraryRepository {
    constructor(context: Context) : this(context.applicationContext.readerDataStore)

    override suspend fun getRecentDocuments(): List<RecentDocument> =
        readSnapshot().recentDocumentsList.map { it.toDomain() }

    override suspend fun recordDocument(document: RecentDocument) {
        dataStore.updateData { current ->
            val existing = current.recentDocumentsList.map { it.toDomain() }
            val previous = existing.firstOrNull { it.uri == document.uri }
            val merged = document.copy(
                bookmarkedPages = previous?.bookmarkedPages ?: document.bookmarkedPages
            )
            current.withDocuments(
                normalizeDocuments(listOf(merged) + existing.filterNot { it.uri == merged.uri })
            )
        }
    }

    override suspend fun updateProgress(uri: String, pageIndex: Int) {
        dataStore.updateData { current ->
            val updated = current.recentDocumentsList.map { it.toDomain() }
                .map { document ->
                    if (document.uri == uri) {
                        document.copy(
                            lastPage = pageIndex.coerceIn(
                                0,
                                (document.pageCount - 1).coerceAtLeast(0)
                            ),
                            lastOpenedAt = System.currentTimeMillis()
                        )
                    } else {
                        document
                    }
                }
            current.withDocuments(normalizeDocuments(updated))
        }
    }

    override suspend fun toggleBookmark(uri: String, pageIndex: Int): Set<Int> {
        val updatedData = dataStore.updateData { current ->
            val updated = current.recentDocumentsList.map { it.toDomain() }
                .map { document ->
                    if (document.uri != uri || pageIndex !in 0 until document.pageCount) {
                        document
                    } else {
                        val bookmarks = if (pageIndex in document.bookmarkedPages) {
                            document.bookmarkedPages - pageIndex
                        } else {
                            document.bookmarkedPages + pageIndex
                        }
                        document.copy(bookmarkedPages = bookmarks)
                    }
                }
            current.withDocuments(normalizeDocuments(updated))
        }
        return updatedData.recentDocumentsList
            .firstOrNull { it.uri == uri }
            ?.bookmarkedPagesList
            ?.toSet()
            .orEmpty()
    }

    override suspend fun clearRecentDocuments() {
        dataStore.updateData { current -> current.toBuilder().clearRecentDocuments().build() }
    }

    override suspend fun getPreferences(): ReaderPreferences =
        readSnapshot().preferences.toDomain()

    override suspend fun savePreferences(preferences: ReaderPreferences) {
        dataStore.updateData { current ->
            current.toBuilder().setPreferences(preferences.toProto()).build()
        }
    }

    private suspend fun readSnapshot(): ReaderDataProto = try {
        dataStore.data.first()
    } catch (_: IOException) {
        ReaderDataSchema.defaultValue()
    }

    private fun ReaderDataProto.withDocuments(documents: List<RecentDocument>): ReaderDataProto =
        toBuilder()
            .clearRecentDocuments()
            .addAllRecentDocuments(documents.map { it.toProto() })
            .build()

    private fun normalizeDocuments(documents: List<RecentDocument>): List<RecentDocument> =
        documents.asSequence()
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.uri }
            .sortedByDescending { it.lastOpenedAt }
            .take(ReaderDataSchema.MAX_RECENT_DOCUMENTS)
            .toList()

    private fun RecentDocumentProto.toDomain(): RecentDocument = RecentDocument(
        uri = uri,
        title = title.ifBlank { "Document" },
        pageCount = pageCount.coerceAtLeast(0),
        lastPage = lastPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
        lastOpenedAt = lastOpenedAt,
        bookmarkedPages = bookmarkedPagesList.filter { it in 0 until pageCount }.toSet()
    )

    private fun RecentDocument.toProto(): RecentDocumentProto {
        val safePageCount = pageCount.coerceAtLeast(0)
        return RecentDocumentProto.newBuilder()
            .setUri(uri)
            .setTitle(title.ifBlank { "Document" })
            .setPageCount(safePageCount)
            .setLastPage(lastPage.coerceIn(0, (safePageCount - 1).coerceAtLeast(0)))
            .setLastOpenedAt(lastOpenedAt)
            .addAllBookmarkedPages(bookmarkedPages.filter { it in 0 until safePageCount }.sorted())
            .build()
    }

    private fun ReaderPreferencesProto.toDomain(): ReaderPreferences = ReaderPreferences(
        themeMode = when (themeMode) {
            ThemeModeProto.THEME_MODE_LIGHT -> ThemeMode.Light
            ThemeModeProto.THEME_MODE_DARK -> ThemeMode.Dark
            else -> ThemeMode.System
        },
        keepScreenOn = keepScreenOn,
        speechRate = speechRate.takeIf { it in 0.6f..1.6f } ?: 1f
    )

    private fun ReaderPreferences.toProto(): ReaderPreferencesProto =
        ReaderPreferencesProto.newBuilder()
            .setThemeMode(
                when (themeMode) {
                    ThemeMode.System -> ThemeModeProto.THEME_MODE_SYSTEM
                    ThemeMode.Light -> ThemeModeProto.THEME_MODE_LIGHT
                    ThemeMode.Dark -> ThemeModeProto.THEME_MODE_DARK
                }
            )
            .setKeepScreenOn(keepScreenOn)
            .setSpeechRate(speechRate.coerceIn(0.6f, 1.6f))
            .build()
}
