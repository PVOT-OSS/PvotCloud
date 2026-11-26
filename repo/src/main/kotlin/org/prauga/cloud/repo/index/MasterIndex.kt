// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.index

import kotlinx.serialization.SerializationException
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.ResticJson
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.pack.PackedBlob

class MasterIndex private constructor() {

    private data class Key(val type: BlobType, val id: Id)

    private val entries = HashMap<Key, BlobLocation>()
    private val pending = LinkedHashMap<Id, List<PackedBlob>>()
    private val packs = HashMap<Id, List<PackedBlob>>()

    val size: Int
        get() = entries.size

    val hasPending: Boolean
        get() = pending.isNotEmpty()

    val pendingPacks: Int
        get() = pending.size

    val packIds: Set<Id>
        get() = packs.keys.toSet()

    fun blobsIn(packId: Id): List<PackedBlob>? = packs[packId]

    fun contains(type: BlobType, id: Id): Boolean = Key(type, id) in entries

    fun lookup(type: BlobType, id: Id): BlobLocation? = entries[Key(type, id)]

    fun addPack(packId: Id, blobs: List<PackedBlob>) {
        index(packId, blobs)
        pending[packId] = blobs
    }

    fun save(repository: Repository): List<String> {
        val names = mutableListOf<String>()
        val batch = mutableListOf<IndexJson.Pack>()
        var blobCount = 0
        fun flush() {
            if (batch.isEmpty()) return
            val json =
                ResticJson.encodeToString(IndexJson.serializer(), IndexJson(packs = batch.toList()))
            names += repository.saveUnpacked(FileType.INDEX, json.toByteArray())
            batch.clear()
            blobCount = 0
        }
        for ((packId, blobs) in pending) {
            if (blobCount > 0 && blobCount + blobs.size > MAX_BLOBS_PER_FILE) flush()
            batch += IndexJson.Pack(packId.hex, blobs.map(::toJson))
            blobCount += blobs.size
        }
        flush()
        pending.clear()
        return names
    }

    fun recoverOrphans(repository: Repository): OrphanRecovery {
        val recovered = mutableListOf<Id>()
        val damaged = mutableListOf<Id>()
        val orphans =
            repository.list(FileType.PACK).mapNotNull(Id::parseOrNull).filterNot { it in packs }
        for (id in orphans) {
            try {
                addPack(id, repository.readPackHeader(id))
                recovered += id
            } catch (_: RepositoryException.Corrupted) {
                damaged += id
            }
        }
        return OrphanRecovery(recovered, damaged)
    }

    private fun index(packId: Id, blobs: List<PackedBlob>) {
        packs[packId] = blobs
        for (blob in blobs) entries.putIfAbsent(Key(blob.type, blob.id), BlobLocation(packId, blob))
    }

    companion object {
        private const val MAX_BLOBS_PER_FILE = 50_000
        private const val DATA = "data"
        private const val TREE = "tree"

        fun empty(): MasterIndex = MasterIndex()

        fun load(repository: Repository): MasterIndex {
            val files =
                repository.list(FileType.INDEX).associateWith { name ->
                    val bytes = repository.loadUnpacked(FileType.INDEX, name)
                    try {
                        ResticJson.decodeFromString(IndexJson.serializer(), bytes.decodeToString())
                    } catch (_: SerializationException) {
                        throw RepositoryException.Corrupted(FileType.INDEX, name)
                    }
                }
            val superseded = files.values.flatMap { it.supersedes }.toSet()
            val index = MasterIndex()
            for ((name, file) in files) {
                if (name in superseded) continue
                for (pack in file.packs) {
                    val packId = parseId(pack.id, name)
                    index.index(packId, pack.blobs.map { fromJson(it, name) })
                }
            }
            return index
        }

        private fun toJson(blob: PackedBlob) =
            IndexJson.Blob(
                id = blob.id.hex,
                type = if (blob.type == BlobType.TREE) TREE else DATA,
                offset = blob.offset,
                length = blob.length,
                uncompressedLength = blob.uncompressedLength,
            )

        private fun fromJson(blob: IndexJson.Blob, file: String): PackedBlob {
            val type =
                when (blob.type) {
                    DATA -> BlobType.DATA
                    TREE -> BlobType.TREE
                    else -> throw RepositoryException.Corrupted(FileType.INDEX, file)
                }
            if (blob.offset < 0 || blob.length < 0 || (blob.uncompressedLength ?: 1) <= 0) {
                throw RepositoryException.Corrupted(FileType.INDEX, file)
            }
            return PackedBlob(
                type,
                parseId(blob.id, file),
                blob.offset,
                blob.length,
                blob.uncompressedLength,
            )
        }

        private fun parseId(hex: String, file: String): Id =
            try {
                Id(hex)
            } catch (_: IllegalArgumentException) {
                throw RepositoryException.Corrupted(FileType.INDEX, file)
            }
    }
}
