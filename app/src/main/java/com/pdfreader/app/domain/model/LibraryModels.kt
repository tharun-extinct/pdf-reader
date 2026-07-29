package com.pdfreader.app.domain.model

/**
 * Lightweight metadata for a document the user has opened before.
 *
 * The URI is stored as a String so this domain model does not depend on Android
 * framework types. The persisted SAF permission remains the source of access.
 */
data class RecentDocument(
    val uri: String,
    val title: String,
    val pageCount: Int,
    val lastPage: Int,
    val lastOpenedAt: Long,
    val bookmarkedPages: Set<Int> = emptySet()
) {
    val progress: Float
        get() = if (pageCount <= 1) 0f else lastPage.toFloat() / (pageCount - 1).toFloat()
}

enum class ThemeMode {
    System,
    Light,
    Dark
}

data class ReaderPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val keepScreenOn: Boolean = false,
    val speechRate: Float = 1f
)
