package com.pdfreader.app.data.preferences

import com.pdfreader.app.data.preferences.proto.ReaderDataProto
import com.pdfreader.app.data.preferences.proto.ThemeModeProto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderDataMigrationsTest {
    @Test
    fun legacyDataMapsHistoryAndPreferencesBeforeSchemaUpgrade() = runBlocking {
        val legacyJson = """
            [
              {
                "uri": "content://documents/one.pdf",
                "title": "One",
                "pageCount": 10,
                "lastPage": 4,
                "lastOpenedAt": 1234,
                "bookmarkedPages": [2, 8, 99]
              }
            ]
        """.trimIndent()

        val legacyMapped = ReaderDataMigrations.migrateLegacyData(
            currentData = ReaderDataProto.getDefaultInstance(),
            recentDocumentsJson = legacyJson,
            themeMode = "Dark",
            keepScreenOn = true,
            speechRate = 1.4f
        )
        val migrated = ReaderDataV0ToV1Migration.migrate(legacyMapped)

        assertEquals(ReaderDataSchema.CURRENT_VERSION, migrated.schemaVersion)
        assertEquals(1, migrated.recentDocumentsCount)
        assertEquals("content://documents/one.pdf", migrated.recentDocumentsList.single().uri)
        assertEquals(listOf(2, 8), migrated.recentDocumentsList.single().bookmarkedPagesList)
        assertEquals(ThemeModeProto.THEME_MODE_DARK, migrated.preferences.themeMode)
        assertEquals(true, migrated.preferences.keepScreenOn)
        assertEquals(1.4f, migrated.preferences.speechRate)
    }

    @Test
    fun schemaMigrationAddsDefaultsAndIsIdempotent() = runBlocking {
        val migrated = ReaderDataV0ToV1Migration.migrate(ReaderDataProto.getDefaultInstance())

        assertEquals(ReaderDataSchema.CURRENT_VERSION, migrated.schemaVersion)
        assertEquals(ThemeModeProto.THEME_MODE_SYSTEM, migrated.preferences.themeMode)
        assertEquals(1f, migrated.preferences.speechRate)
        assertFalse(ReaderDataV0ToV1Migration.shouldMigrate(migrated))

        val retriedLegacyMigration = ReaderDataMigrations.migrateLegacyData(
            currentData = migrated,
            recentDocumentsJson = "[]",
            themeMode = "Dark",
            keepScreenOn = true,
            speechRate = 1.6f
        )
        assertEquals(migrated, retriedLegacyMigration)
    }

    @Test
    fun legacyDocumentWithoutTitleRemainsBlankForLocalizedPresentation() {
        val migrated = ReaderDataMigrations.migrateLegacyData(
            currentData = ReaderDataProto.getDefaultInstance(),
            recentDocumentsJson = """[{"uri":"content://documents/untitled.pdf"}]""",
            themeMode = null,
            keepScreenOn = false,
            speechRate = 1f
        )

        assertEquals("", migrated.recentDocumentsList.single().title)
    }
}
