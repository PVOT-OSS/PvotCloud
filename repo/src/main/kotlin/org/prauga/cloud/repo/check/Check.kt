// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.check

import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.snapshot.Snapshots
import org.prauga.cloud.repo.tree.Tree

class Check(private val repository: Repository, private val index: MasterIndex) {

    fun run(): CheckResult {
        val problems = mutableListOf<CheckProblem>()
        val stored = repository.list(FileType.PACK).mapNotNull(Id::parseOrNull).toSet()
        val indexed = index.packIds
        (indexed - stored).mapTo(problems, CheckProblem::MissingPack)
        (stored - indexed).mapTo(problems, CheckProblem::UnindexedPack)
        val damaged = (indexed - stored).toMutableSet()

        val visited = HashSet<Id>()
        val snapshotIds = repository.list(FileType.SNAPSHOT).mapNotNull(Id::parseOrNull)
        val snapshots = Snapshots(repository)
        for (snapshotId in snapshotIds) {
            val root =
                try {
                    snapshots.load(snapshotId).tree
                } catch (_: RepositoryException.Corrupted) {
                    problems += CheckProblem.UnreadableSnapshot(snapshotId)
                    continue
                }
            walk(snapshotId, root, visited, damaged, problems)
        }
        return CheckResult(
            snapshots = snapshotIds.size,
            trees = visited.size,
            packs = stored.size,
            problems = problems.distinct(),
        )
    }

    private fun walk(
        snapshotId: Id,
        root: Id,
        visited: MutableSet<Id>,
        damaged: MutableSet<Id>,
        problems: MutableList<CheckProblem>,
    ) {
        val pending = ArrayDeque(listOf(root))
        while (pending.isNotEmpty()) {
            val treeId = pending.removeFirst()
            if (!visited.add(treeId)) continue
            val location = index.lookup(BlobType.TREE, treeId)
            if (location == null) {
                problems += CheckProblem.MissingBlob(snapshotId, BlobType.TREE, treeId)
                continue
            }
            if (location.packId in damaged) continue
            val tree =
                try {
                    Tree.decode(repository.readBlob(location.packId, location.blob))
                } catch (_: RepositoryException.Corrupted) {
                    damaged += location.packId
                    problems += CheckProblem.DamagedPack(location.packId)
                    continue
                }
            for (node in tree.nodes) {
                node.subtree?.let(pending::addLast)
                for (blob in node.content.orEmpty()) {
                    if (!index.contains(BlobType.DATA, blob)) {
                        problems += CheckProblem.MissingBlob(snapshotId, BlobType.DATA, blob)
                    }
                }
            }
        }
    }
}
