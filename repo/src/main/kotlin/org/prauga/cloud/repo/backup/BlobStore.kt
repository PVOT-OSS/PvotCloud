// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.backup

import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.pack.PackWriter

class BlobStore(
    private val repository: Repository,
    private val index: MasterIndex,
    private val packSize: Int = DEFAULT_PACK_SIZE,
    private val checkpointPacks: Int = DEFAULT_CHECKPOINT_PACKS,
) {

    private val writers = mutableMapOf<BlobType, PackWriter>()
    private val unsaved = mutableMapOf<BlobType, MutableSet<Id>>()

    var newBlobs: Int = 0
        private set

    var newPacks: Int = 0
        private set

    fun save(type: BlobType, plaintext: ByteArray): Id {
        val id = Id.hash(plaintext)
        if (index.contains(type, id) || id in unsaved.getOrPut(type, ::mutableSetOf)) return id
        val writer = writers.getOrPut(type) { repository.newPackWriter(type) }
        writer.add(plaintext)
        unsaved.getValue(type) += id
        newBlobs++
        if (writer.size >= packSize) finish(type)
        return id
    }

    fun flush() {
        writers.keys.toList().forEach(::finish)
    }

    private fun finish(type: BlobType) {
        val writer = writers.remove(type) ?: return
        val pack = writer.finish()
        repository.savePack(pack)
        index.addPack(pack.id, pack.blobs)
        unsaved.remove(type)
        newPacks++
        if (index.pendingPacks >= checkpointPacks) index.save(repository)
    }

    companion object {
        const val DEFAULT_PACK_SIZE = 16 * 1024 * 1024
        const val DEFAULT_CHECKPOINT_PACKS = 4
    }
}
