// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.storage

import java.io.File
import java.io.FileNotFoundException

class FileDocumentTree(private val root: File) : DocumentTree {

    var calls = 0
        private set

    override val rootId: String = ""

    override fun children(directoryId: String): List<DocumentEntry> {
        calls++
        return file(directoryId).listFiles().orEmpty().map {
            DocumentEntry(id(it), it.name, it.isDirectory)
        }
    }

    override fun createDirectory(parentId: String, name: String): String {
        calls++
        return id(unique(file(parentId), name).apply { mkdir() })
    }

    override fun createFile(parentId: String, name: String): String {
        calls++
        return id(unique(file(parentId), name).apply { createNewFile() })
    }

    override fun write(documentId: String, data: ByteArray) {
        calls++
        file(documentId).writeBytes(data)
    }

    override fun read(documentId: String): ByteArray {
        calls++
        val file = file(documentId)
        if (!file.exists()) throw FileNotFoundException(documentId)
        return file.readBytes()
    }

    override fun rename(documentId: String, name: String): String {
        calls++
        val from = file(documentId)
        val to = unique(checkNotNull(from.parentFile) { "$documentId has no parent" }, name)
        check(from.renameTo(to))
        return id(to)
    }

    override fun delete(documentId: String) {
        calls++
        file(documentId).deleteRecursively()
    }

    private fun file(id: String) = if (id.isEmpty()) root else File(root, id)

    private fun id(file: File) = file.relativeTo(root).path

    private fun unique(parent: File, name: String): File {
        var candidate = File(parent, name)
        var n = 1
        while (candidate.exists()) candidate = File(parent, "$name (${n++})")
        return candidate
    }
}
