// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.restore

import java.io.InputStream
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.tree.Node
import org.prauga.cloud.repo.tree.NodeType
import org.prauga.cloud.repo.tree.Tree

class Restore(private val repository: Repository, private val index: MasterIndex) {

    fun entries(tree: Id): Sequence<RestoreEntry> = sequence { walk(tree, "") }

    fun open(node: Node): InputStream {
        require(node.type == NodeType.FILE) { "${node.name} is not a file" }
        val content = node.content.orEmpty()
        return ContentStream(content.iterator(), node.size) { load(BlobType.DATA, it) }
    }

    private suspend fun SequenceScope<RestoreEntry>.walk(tree: Id, parent: String) {
        for (node in Tree.decode(load(BlobType.TREE, tree)).nodes) {
            val path = if (parent.isEmpty()) node.name else "$parent/${node.name}"
            yield(RestoreEntry(path, node))
            node.subtree?.let { walk(it, path) }
        }
    }

    private fun load(type: BlobType, id: Id): ByteArray {
        val location = index.lookup(type, id) ?: throw RepositoryException.MissingBlob(id)
        return repository.readBlob(location.packId, location.blob)
    }
}
