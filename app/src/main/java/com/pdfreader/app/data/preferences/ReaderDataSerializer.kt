package com.pdfreader.app.data.preferences

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.pdfreader.app.data.preferences.proto.ReaderDataProto
import java.io.InputStream
import java.io.OutputStream

object ReaderDataSerializer : Serializer<ReaderDataProto> {
    // Keep the serializer default at schema version 0 so migrations also run for
    // a first install and, critically, before importing legacy preferences.
    override val defaultValue: ReaderDataProto = ReaderDataProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ReaderDataProto = try {
        ReaderDataProto.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Unable to read the reader-data schema.", exception)
    }

    override suspend fun writeTo(t: ReaderDataProto, output: OutputStream) {
        t.writeTo(output)
    }
}
