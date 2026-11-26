// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.snapshot

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.tree.GoStrings

class Snapshots(private val repository: Repository) {

    fun save(snapshot: Snapshot): Id {
        val json =
            SnapshotJson(
                time = GoStrings.time(snapshot.time),
                parent = snapshot.parent?.hex,
                tree = snapshot.tree.hex,
                paths = snapshot.paths,
                hostname = snapshot.hostname,
                username = snapshot.username,
                tags = snapshot.tags,
                original = snapshot.original?.hex,
                programVersion = snapshot.programVersion,
            )
        val bytes = JSON.encodeToString(SnapshotJson.serializer(), json).toByteArray()
        return Id(repository.saveUnpacked(FileType.SNAPSHOT, bytes))
    }

    fun load(id: Id): Snapshot {
        val bytes = repository.loadUnpacked(FileType.SNAPSHOT, id.hex)
        return try {
            val json = JSON.decodeFromString(SnapshotJson.serializer(), bytes.decodeToString())
            Snapshot(
                time = OffsetDateTime.parse(json.time),
                tree = Id(json.tree),
                paths = json.paths,
                hostname = json.hostname,
                username = json.username,
                tags = json.tags,
                parent = json.parent?.let(::Id),
                original = json.original?.let(::Id),
                programVersion = json.programVersion,
            )
        } catch (_: SerializationException) {
            throw RepositoryException.Corrupted(FileType.SNAPSHOT, id.hex)
        } catch (_: DateTimeParseException) {
            throw RepositoryException.Corrupted(FileType.SNAPSHOT, id.hex)
        } catch (_: IllegalArgumentException) {
            throw RepositoryException.Corrupted(FileType.SNAPSHOT, id.hex)
        }
    }

    fun list(): List<SnapshotEntry> =
        repository
            .list(FileType.SNAPSHOT)
            .map { SnapshotEntry(Id(it), load(Id(it))) }
            .sortedBy { it.snapshot.time.toInstant() }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
