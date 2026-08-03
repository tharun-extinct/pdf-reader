package com.pdfreader.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.migrations.SharedPreferencesMigration
import com.pdfreader.app.data.preferences.proto.ReaderDataProto
import com.pdfreader.app.data.preferences.proto.ReaderPreferencesProto
import com.pdfreader.app.data.preferences.proto.RecentDocumentProto
import com.pdfreader.app.data.preferences.proto.ThemeModeProto
import com.pdfreader.app.domain.model.DEFAULT_HIGHLIGHTER_COLORS
import com.pdfreader.app.domain.model.DEFAULT_PEN_COLORS
import org.json.JSONArray

internal object ReaderDataSchema {
    const val CURRENT_VERSION = 1
    const val MAX_RECENT_DOCUMENTS = 20

    fun defaultValue(): ReaderDataProto = ReaderDataProto.newBuilder()
        .setSchemaVersion(CURRENT_VERSION)
        .setPreferences(defaultPreferences())
        .build()

    fun defaultPreferences(): ReaderPreferencesProto = ReaderPreferencesProto.newBuilder()
        .setThemeMode(ThemeModeProto.THEME_MODE_SYSTEM)
        .setSpeechRate(1f)
        .addAllPenColors(DEFAULT_PEN_COLORS)
        .addAllHighlighterColors(DEFAULT_HIGHLIGHTER_COLORS)
        .build()
}

internal object ReaderDataMigrations {
    private const val LEGACY_PREFERENCES_NAME = "nox_reader_preferences"
    private const val KEY_RECENT_DOCUMENTS = "recent_documents"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_SPEECH_RATE = "speech_rate"

    private val legacyKeys = setOf(
        KEY_RECENT_DOCUMENTS,
        KEY_THEME_MODE,
        KEY_KEEP_SCREEN_ON,
        KEY_SPEECH_RATE
    )

    fun create(context: Context): List<DataMigration<ReaderDataProto>> = listOf(
        SharedPreferencesMigration(
            context,
            LEGACY_PREFERENCES_NAME,
            legacyKeys
        ) { sharedPreferences, currentData ->
            migrateLegacyData(
                currentData = currentData,
                recentDocumentsJson = sharedPreferences.getString(KEY_RECENT_DOCUMENTS, null),
                themeMode = sharedPreferences.getString(KEY_THEME_MODE, null),
                keepScreenOn = sharedPreferences.getBoolean(KEY_KEEP_SCREEN_ON, false),
                speechRate = sharedPreferences.getFloat(KEY_SPEECH_RATE, 1f)
            )
        },
        ReaderDataV0ToV1Migration
    )

    internal fun migrateLegacyData(
        currentData: ReaderDataProto,
        recentDocumentsJson: String?,
        themeMode: String?,
        keepScreenOn: Boolean,
        speechRate: Float
    ): ReaderDataProto {
        // A completed schema migration proves the legacy values were already
        // incorporated. This keeps retries idempotent if legacy cleanup fails.
        if (currentData.schemaVersion >= ReaderDataSchema.CURRENT_VERSION) {
            return currentData
        }

        val builder = currentData.toBuilder()
        if (currentData.recentDocumentsCount == 0) {
            builder.addAllRecentDocuments(decodeLegacyDocuments(recentDocumentsJson))
        }
        if (!currentData.hasPreferences()) {
            builder.preferences = ReaderPreferencesProto.newBuilder()
                .setThemeMode(themeMode.toProtoThemeMode())
                .setKeepScreenOn(keepScreenOn)
                .setSpeechRate(speechRate.coerceIn(0.6f, 1.6f))
                .build()
        }
        return builder.build()
    }

    private fun decodeLegacyDocuments(rawJson: String?): List<RecentDocumentProto> {
        if (rawJson.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = runCatching { array.getJSONObject(index) }.getOrNull() ?: continue
                    val uri = item.optString("uri").trim()
                    if (uri.isEmpty()) continue

                    val pageCount = item.optInt("pageCount", 0).coerceAtLeast(0)
                    val lastPage = item.optInt("lastPage", 0)
                        .coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                    val bookmarksArray = item.optJSONArray("bookmarkedPages") ?: JSONArray()
                    val bookmarks = buildSet {
                        for (bookmarkIndex in 0 until bookmarksArray.length()) {
                            val pageIndex = bookmarksArray.optInt(bookmarkIndex, -1)
                            if (pageIndex in 0 until pageCount) add(pageIndex)
                        }
                    }

                    add(
                        RecentDocumentProto.newBuilder()
                            .setUri(uri)
                            .setTitle(item.optString("title", ""))
                            .setPageCount(pageCount)
                            .setLastPage(lastPage)
                            .setLastOpenedAt(item.optLong("lastOpenedAt", 0L))
                            .addAllBookmarkedPages(bookmarks.sorted())
                            .build()
                    )
                }
            }.distinctBy { it.uri }
                .sortedByDescending { it.lastOpenedAt }
                .take(ReaderDataSchema.MAX_RECENT_DOCUMENTS)
        }.getOrDefault(emptyList())
    }

    private fun String?.toProtoThemeMode(): ThemeModeProto = when (this) {
        "Light" -> ThemeModeProto.THEME_MODE_LIGHT
        "Dark" -> ThemeModeProto.THEME_MODE_DARK
        else -> ThemeModeProto.THEME_MODE_SYSTEM
    }
}

internal object ReaderDataV0ToV1Migration : DataMigration<ReaderDataProto> {
    override suspend fun shouldMigrate(currentData: ReaderDataProto): Boolean =
        currentData.schemaVersion < ReaderDataSchema.CURRENT_VERSION

    override suspend fun migrate(currentData: ReaderDataProto): ReaderDataProto {
        val preferences = if (currentData.hasPreferences()) {
            val builder = currentData.preferences.toBuilder()
                .setThemeMode(
                    currentData.preferences.themeMode.takeUnless {
                        it == ThemeModeProto.THEME_MODE_UNSPECIFIED ||
                            it == ThemeModeProto.UNRECOGNIZED
                    } ?: ThemeModeProto.THEME_MODE_SYSTEM
                )
                .setSpeechRate(
                    currentData.preferences.speechRate
                        .takeIf { it in 0.6f..1.6f }
                        ?: 1f
                )
            if (currentData.preferences.penColorsCount != DEFAULT_PEN_COLORS.size) {
                builder.clearPenColors().addAllPenColors(DEFAULT_PEN_COLORS)
            }
            if (currentData.preferences.highlighterColorsCount != DEFAULT_HIGHLIGHTER_COLORS.size) {
                builder.clearHighlighterColors().addAllHighlighterColors(DEFAULT_HIGHLIGHTER_COLORS)
            }
            builder.build()
        } else {
            ReaderDataSchema.defaultPreferences()
        }

        return currentData.toBuilder()
            .setSchemaVersion(ReaderDataSchema.CURRENT_VERSION)
            .setPreferences(preferences)
            .build()
    }

    override suspend fun cleanUp() = Unit
}
