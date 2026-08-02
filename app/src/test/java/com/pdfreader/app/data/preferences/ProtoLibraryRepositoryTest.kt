package com.pdfreader.app.data.preferences

import androidx.datastore.core.DataStoreFactory
import com.pdfreader.app.domain.model.ReaderPreferences
import com.pdfreader.app.domain.model.RecentDocument
import com.pdfreader.app.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProtoLibraryRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun repositoryPersistsBoundedHistoryBookmarksAndPreferences() = runBlocking {
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = DataStoreFactory.create(
            serializer = ReaderDataSerializer,
            migrations = listOf(ReaderDataV0ToV1Migration),
            scope = dataStoreScope,
            produceFile = { File(temporaryFolder.root, "reader-data.pb") }
        )
        val repository = ProtoLibraryRepository(dataStore)

        try {
            repeat(22) { index ->
                repository.recordDocument(
                    RecentDocument(
                        uri = "content://documents/$index.pdf",
                        title = "Document $index",
                        pageCount = 10,
                        lastPage = index % 10,
                        lastOpenedAt = index.toLong()
                    )
                )
            }

            val recentDocuments = repository.getRecentDocuments()
            assertEquals(20, recentDocuments.size)
            assertEquals("content://documents/21.pdf", recentDocuments.first().uri)
            assertFalse(recentDocuments.any { it.uri == "content://documents/0.pdf" })

            val bookmarks = repository.toggleBookmark("content://documents/21.pdf", 3)
            assertEquals(setOf(3), bookmarks)
            assertTrue(repository.getRecentDocuments().first().bookmarkedPages.contains(3))

            val preferences = ReaderPreferences(
                themeMode = ThemeMode.Dark,
                keepScreenOn = true,
                speechRate = 1.4f
            )
            repository.savePreferences(preferences)
            assertEquals(preferences, repository.getPreferences())

            repository.clearRecentDocuments()
            assertTrue(repository.getRecentDocuments().isEmpty())
            assertEquals(preferences, repository.getPreferences())

            repository.recordDocument(
                RecentDocument(
                    uri = "content://documents/untitled.pdf",
                    title = "",
                    pageCount = 1,
                    lastOpenedAt = 100L
                )
            )
            assertEquals("", repository.getRecentDocuments().single().title)
        } finally {
            dataStoreScope.cancel()
        }
    }
}
