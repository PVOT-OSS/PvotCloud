// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.check

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.InMemoryBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.backup.Backup
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.source.MemoryDirectory
import org.prauga.cloud.repo.source.MemoryFile

class CheckTest {

    private val backend = InMemoryBackend()
    private val repository =
        Repository.create(
            backend,
            ZstdCompressor(),
            "phrase",
            "phone",
            "user",
            KdfParams(1024, 8, 1),
        )

    private fun backup() {
        val source =
            MemoryDirectory(
                "root",
                listOf(
                    MemoryDirectory(
                        "DCIM",
                        listOf(MemoryFile("a.jpg", Random(1).nextBytes(200 * 1024))),
                    ),
                    MemoryFile("notes.txt", "hello".toByteArray()),
                ),
            )
        Backup(repository, MasterIndex.load(repository), packSize = 64 * 1024)
            .run(source, "phone", listOf("/"))
    }

    private fun check() = Check(repository, MasterIndex.load(repository)).run()

    @Test
    fun healthyRepositoryHasNoProblems() {
        backup()
        backup()

        val result = check()

        assertTrue(result.problems.toString(), result.ok)
        assertEquals(2, result.snapshots)
        assertEquals(2, result.trees)
    }

    @Test
    fun deletedPackIsReported() {
        backup()
        val dataPack =
            checkNotNull(
                MasterIndex.load(repository).packIds.firstOrNull { pack ->
                    MasterIndex.load(repository).blobsIn(pack).orEmpty().all {
                        it.type == BlobType.DATA
                    }
                }
            )
        backend.delete(FileType.PACK, dataPack.hex)

        val result = check()

        assertEquals(listOf(CheckProblem.MissingPack(dataPack)), result.problems)
    }

    @Test
    fun packWithoutIndexIsReported() {
        backup()
        val pack =
            repository.newPackWriter(BlobType.DATA).apply { add("stray".toByteArray()) }.finish()
        repository.savePack(pack)

        assertEquals(listOf(CheckProblem.UnindexedPack(pack.id)), check().problems)
    }

    @Test
    fun damagedTreePackIsReported() {
        backup()
        val index = MasterIndex.load(repository)
        val treePack =
            index.packIds.first { pack ->
                index.blobsIn(pack).orEmpty().any { it.type == BlobType.TREE }
            }
        backend.corrupt(FileType.PACK, treePack.hex)

        assertEquals(listOf(CheckProblem.DamagedPack(treePack)), check().problems)
    }

    @Test
    fun dataBlobMissingFromTheIndexIsReported() {
        backup()
        val index = MasterIndex.load(repository)
        val dataPacks =
            index.packIds.filter { pack ->
                index.blobsIn(pack).orEmpty().all { it.type == BlobType.DATA }
            }
        repository.list(FileType.INDEX).forEach { backend.delete(FileType.INDEX, it) }
        val trimmed = MasterIndex.empty()
        index.packIds
            .filterNot { it in dataPacks }
            .forEach { trimmed.addPack(it, checkNotNull(index.blobsIn(it))) }
        trimmed.save(repository)
        dataPacks.forEach { backend.delete(FileType.PACK, it.hex) }

        val problems = check().problems

        assertTrue(problems.isNotEmpty())
        assertTrue(problems.all { it is CheckProblem.MissingBlob && it.type == BlobType.DATA })
    }

    @Test
    fun readingAllDataVerifiesEveryPack() {
        backup()

        val result = Check(repository, MasterIndex.load(repository)).run(readDataPercent = 100)

        assertTrue(result.problems.toString(), result.ok)
        assertEquals(result.packs, result.packsRead)
    }

    @Test
    fun damagedDataIsOnlyFoundWhenReadingData() {
        backup()
        val index = MasterIndex.load(repository)
        val dataPack =
            index.packIds.first { pack ->
                index.blobsIn(pack).orEmpty().all { it.type == BlobType.DATA }
            }
        backend.corrupt(FileType.PACK, dataPack.hex)

        assertTrue(check().ok)
        val result = Check(repository, MasterIndex.load(repository)).run(readDataPercent = 100)
        assertEquals(listOf(CheckProblem.DamagedPack(dataPack)), result.problems)
    }

    @Test
    fun readDataSamplesTheRequestedShare() {
        backup()
        val index = MasterIndex.load(repository)

        val result = Check(repository, index).run(readDataPercent = 50, random = Random(5))

        assertEquals((index.packIds.size + 1) / 2, result.packsRead)
    }

    @Test
    fun headerThatDisagreesWithTheIndexIsReported() {
        backup()
        val pack =
            repository
                .newPackWriter(BlobType.DATA)
                .apply {
                    add("first".toByteArray())
                    add("second".toByteArray())
                }
                .finish()
        repository.savePack(pack)
        MasterIndex.empty().apply { addPack(pack.id, pack.blobs.take(1)) }.save(repository)

        val result = Check(repository, MasterIndex.load(repository)).run(readDataPercent = 100)

        assertEquals(listOf(CheckProblem.HeaderMismatch(pack.id)), result.problems)
    }
}
