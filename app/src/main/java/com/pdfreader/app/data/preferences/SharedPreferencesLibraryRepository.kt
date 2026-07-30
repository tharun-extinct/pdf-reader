package com.pdfreader.app.data.preferences

import android.content.Context
import com.pdfreader.app.domain.model.ReaderPreferences
import com.pdfreader.app.domain.model.RecentDocument
import com.pdfreader.app.domain.model.ThemeMode
import com.pdfreader.app.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores only small metadata records. All parsing and disk access stays on the
 * IO dispatcher so opening the library never blocks Compose.
 */
class SharedPreferencesLibraryRepository(context: Context) : LibraryRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val writeLock = Any()

    override suspend fun getRecentDocuments(): List<RecentDocument> = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            decodeDocuments(preferences.getString(KEY_RECENT_DOCUMENTS, null))
        }
    }

    override suspend fun recordDocument(document: RecentDocument) = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            val current = decodeDocuments(preferences.getString(KEY_RECENT_DOCUMENTS, null))
            val previous = current.firstOrNull { it.uri == document.uri }
            val merged = document.copy(
                bookmarkedPages = previous?.bookmarkedPages ?: document.bookmarkedPages
            )
            val updated = (listOf(merged) + current.filterNot { it.uri == document.uri })
                .sortedByDescending { it.lastOpenedAt }
                .take(MAX_RECENT_DOCUMENTS)
            saveDocuments(updated)
        }
    }

    override suspend fun updateProgress(uri: String, pageIndex: Int) = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            val updated = decodeDocuments(preferences.getString(KEY_RECENT_DOCUMENTS, null))
                .map { document ->
                    if (document.uri == uri) {
                        document.copy(
                            lastPage = pageIndex.coerceIn(0, (document.pageCount - 1).coerceAtLeast(0)),
                            lastOpenedAt = System.currentTimeMillis()
                        )
                    } else {
                        document
                    }
                }
                .sortedByDescending { it.lastOpenedAt }
            saveDocuments(updated)
        }
    }

    override suspend fun toggleBookmark(uri: String, pageIndex: Int): Set<Int> =
        withContext(Dispatchers.IO) {
            synchronized(writeLock) {
                var result = emptySet<Int>()
                val updated = decodeDocuments(preferences.getString(KEY_RECENT_DOCUMENTS, null))
                    .map { document ->
                        if (document.uri != uri) {
                            document
                        } else {
                            result = if (pageIndex in document.bookmarkedPages) {
                                document.bookmarkedPages - pageIndex
                            } else {
                                document.bookmarkedPages + pageIndex
                            }
                            document.copy(bookmarkedPages = result)
                        }
                    }
                saveDocuments(updated)
                result
            }
        }

    override suspend fun clearRecentDocuments() = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            preferences.edit().remove(KEY_RECENT_DOCUMENTS).commit()
            Unit
        }
    }

    override suspend fun getPreferences(): ReaderPreferences = withContext(Dispatchers.IO) {
        val themeMode = runCatching {
            ThemeMode.valueOf(
                preferences.getString(KEY_THEME_MODE, ThemeMode.System.name)
                    ?: ThemeMode.System.name
            )
        }.getOrDefault(ThemeMode.System)

        ReaderPreferences(
            themeMode = themeMode,
            keepScreenOn = preferences.getBoolean(KEY_KEEP_SCREEN_ON, false),
            speechRate = preferences.getFloat(KEY_SPEECH_RATE, 1f).coerceIn(0.6f, 1.6f)
        )
    }

    override suspend fun savePreferences(readerPreferences: ReaderPreferences) =
        withContext(Dispatchers.IO) {
            preferences.edit()
                .putString(KEY_THEME_MODE, readerPreferences.themeMode.name)
                .putBoolean(KEY_KEEP_SCREEN_ON, readerPreferences.keepScreenOn)
                .putFloat(KEY_SPEECH_RATE, readerPreferences.speechRate.coerceIn(0.6f, 1.6f))
                .commit()
            Unit
        }

    private fun saveDocuments(documents: List<RecentDocument>) {
        val json = JSONArray()
        documents.forEach { document ->
            json.put(
                JSONObject()
                    .put("uri", document.uri)
                    .put("title", document.title)
                    .put("pageCount", document.pageCount)
                    .put("lastPage", document.lastPage)
                    .put("lastOpenedAt", document.lastOpenedAt)
                    .put("bookmarkedPages", JSONArray(document.bookmarkedPages.sorted()))
            )
        }
        preferences.edit().putString(KEY_RECENT_DOCUMENTS, json.toString()).commit()
    }

    private fun decodeDocuments(rawJson: String?): List<RecentDocument> {
        if (rawJson.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val bookmarksArray = item.optJSONArray("bookmarkedPages") ?: JSONArray()
                    val bookmarks = buildSet {
                        for (bookmarkIndex in 0 until bookmarksArray.length()) {
                            add(bookmarksArray.optInt(bookmarkIndex))
                        }
                    }
                    add(
                        RecentDocument(
                            uri = item.getString("uri"),
                            title = item.optString("title", "Document"),
                            pageCount = item.optInt("pageCount", 0),
                            lastPage = item.optInt("lastPage", 0),
                            lastOpenedAt = item.optLong("lastOpenedAt", 0L),
                            bookmarkedPages = bookmarks
                        )
                    )
                }
            }.sortedByDescending { it.lastOpenedAt }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFERENCES_NAME = "nox_reader_preferences"
        const val KEY_RECENT_DOCUMENTS = "recent_documents"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_SPEECH_RATE = "speech_rate"
        const val MAX_RECENT_DOCUMENTS = 20
    }
}
