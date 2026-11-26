// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.backup

import java.time.Clock
import java.time.OffsetDateTime
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.chunker.Chunker
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.snapshot.Snapshot
import org.prauga.cloud.repo.snapshot.Snapshots
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceFile
import org.prauga.cloud.repo.source.SourceSymlink
import org.prauga.cloud.repo.tree.Node
import org.prauga.cloud.repo.tree.NodeType
import org.prauga.cloud.repo.tree.Tree

class Backup(
    private val repository: Repository,
    private val index: MasterIndex,
    private val packSize: Int = BlobStore.DEFAULT_PACK_SIZE,
    private val checkpointPacks: Int = BlobStore.DEFAULT_CHECKPOINT_PACKS,
    private val clock: Clock = Clock.systemUTC(),
) {

    private class Counters(val onProgress: (BackupProgress) -> Unit) {
        var files = 0
        var directories = 0
        var bytes = 0L
    }

    fun run(
        root: SourceDirectory,
        hostname: String,
        paths: List<String>,
        tags: List<String> = emptyList(),
        parent: Id? = null,
        programVersion: String? = null,
        progress: (BackupProgress) -> Unit = {},
    ): BackupResult {
        val start = OffsetDateTime.now(clock)
        val orphans = index.recoverOrphans(repository)
        val store = BlobStore(repository, index, packSize, checkpointPacks)
        val chunker = Chunker(repository.chunkerPolynomial)
        val counters = Counters(progress)
        val tree = saveDirectory(root, store, chunker, counters)
        store.flush()
        if (index.hasPending) index.save(repository)
        val snapshot =
            Snapshot(
                time = start,
                tree = tree,
                paths = paths,
                hostname = hostname,
                tags = tags,
                parent = parent,
                programVersion = programVersion,
            )
        val snapshotId = Snapshots(repository).save(snapshot)
        return BackupResult(
            snapshotId = snapshotId,
            tree = tree,
            files = counters.files,
            directories = counters.directories,
            bytes = counters.bytes,
            newBlobs = store.newBlobs,
            newPacks = store.newPacks,
            recoveredPacks = orphans.recovered.size,
            damagedPacks = orphans.damaged,
        )
    }

    private fun saveDirectory(
        directory: SourceDirectory,
        store: BlobStore,
        chunker: Chunker,
        counters: Counters,
    ): Id {
        val nodes =
            directory.children().map { entry ->
                when (entry) {
                    is SourceFile -> saveFile(entry, store, chunker, counters)
                    is SourceDirectory -> {
                        counters.directories++
                        Node(
                            name = entry.name,
                            type = NodeType.DIR,
                            modTime = entry.modTime,
                            mode = Node.MODE_DIR or DIRECTORY_PERMISSIONS,
                            subtree = saveDirectory(entry, store, chunker, counters),
                        )
                    }
                    is SourceSymlink ->
                        Node(
                            name = entry.name,
                            type = NodeType.SYMLINK,
                            modTime = entry.modTime,
                            mode = Node.MODE_SYMLINK or SYMLINK_PERMISSIONS,
                            linkTarget = entry.target,
                        )
                }
            }
        return store.save(BlobType.TREE, Tree(nodes).encode())
    }

    private fun saveFile(
        file: SourceFile,
        store: BlobStore,
        chunker: Chunker,
        counters: Counters,
    ): Node {
        var size = 0L
        val content =
            file.open().use { input ->
                chunker
                    .chunks(input)
                    .map { chunk ->
                        size += chunk.data.size
                        store.save(BlobType.DATA, chunk.data).also {
                            counters.onProgress(
                                BackupProgress(counters.files, counters.bytes + size)
                            )
                        }
                    }
                    .toList()
            }
        counters.files++
        counters.bytes += size
        counters.onProgress(BackupProgress(counters.files, counters.bytes))
        return Node(
            name = file.name,
            type = NodeType.FILE,
            modTime = file.modTime,
            mode = FILE_PERMISSIONS,
            size = size,
            links = 1,
            content = content,
        )
    }

    private companion object {
        const val FILE_PERMISSIONS = 420L
        const val DIRECTORY_PERMISSIONS = 493L
        const val SYMLINK_PERMISSIONS = 511L
    }
}
