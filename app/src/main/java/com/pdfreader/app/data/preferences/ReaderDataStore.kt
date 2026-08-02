package com.pdfreader.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.pdfreader.app.data.preferences.proto.ReaderDataProto

private const val READER_DATA_FILE_NAME = "reader_data.pb"

internal val Context.readerDataStore: DataStore<ReaderDataProto> by dataStore(
    fileName = READER_DATA_FILE_NAME,
    serializer = ReaderDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler {
        ReaderDataSchema.defaultValue()
    },
    produceMigrations = { context -> ReaderDataMigrations.create(context) }
)
