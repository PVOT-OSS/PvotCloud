// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.snapshot

import java.time.OffsetDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.InMemoryBackend
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.crypto.KdfParams

class SnapshotsTest {

    private val repository =
        Repository.create(
            InMemoryBackend(),
            ZstdCompressor(),
            "phrase",
            "phone",
            "user",
            KdfParams(1024, 8, 1),
        )
    private val snapshots = Snapshots(repository)

    private val snapshot =
        Snapshot(
            time = OffsetDateTime.parse("2026-11-26T21:30:15.123456789+05:30"),
            tree = Id.hash("root tree".toByteArray()),
            paths = listOf("/contacts", "/media/DCIM"),
            hostname = "Pixel 9",
            tags = listOf("pvot-cloud"),
            programVersion = "pvot-cloud 1.0.0",
        )

    @Test
    fun savedSnapshotLoadsBack() {
        val id = snapshots.save(snapshot)

        assertEquals(snapshot, snapshots.load(id))
        assertEquals(listOf(SnapshotEntry(id, snapshot)), snapshots.list())
    }

    @Test
    fun usesResticFieldNamesAndOmitsEmptyFields() {
        val id = snapshots.save(snapshot)

        val json =
            Json.parseToJsonElement(
                    repository.loadUnpacked(FileType.SNAPSHOT, id.hex).decodeToString()
                )
                .jsonObject

        assertEquals(
            listOf("time", "tree", "paths", "hostname", "tags", "program_version"),
            json.keys.toList(),
        )
        assertEquals(
            "2026-11-26T21:30:15.123456789+05:30",
            json.getValue("time").jsonPrimitive.content,
        )
        assertEquals("/media/DCIM", json.getValue("paths").jsonArray[1].jsonPrimitive.content)
    }

    @Test
    fun listIsOrderedByTime() {
        val later = snapshot.copy(time = snapshot.time.plusDays(1))
        val laterId = snapshots.save(later)
        val earlierId = snapshots.save(snapshot)

        assertEquals(listOf(earlierId, laterId), snapshots.list().map { it.id })
    }

    @Test(expected = RepositoryException.Corrupted::class)
    fun invalidSnapshotIsRejected() {
        val name =
            repository.saveUnpacked(
                FileType.SNAPSHOT,
                """{"time":"yesterday","tree":"x"}""".toByteArray(),
            )

        snapshots.load(Id(name))
    }
}
