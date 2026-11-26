// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.storage

import java.io.FileNotFoundException
import java.io.IOException
import org.prauga.cloud.repo.Backend
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.RepositoryLayout

class SafBackend(private val tree: DocumentTree) : Backend {

    private val directories = HashMap<String, String>().apply { put("", tree.rootId) }
    private val listings = HashMap<String, MutableMap<String, DocumentEntry>>()
    private var tempCounter = 0L

    override fun save(type: FileType, name: String, data: ByteArray) {
        val (folder, fileName) = split(RepositoryLayout.path(type, name))
        val folderId = checkNotNull(directory(folder, create = true))
        val children = listing(folderId)
        if (fileName in children) return
        val tempId = tree.createFile(folderId, "$TEMP_PREFIX${System.nanoTime()}-${tempCounter++}")
        try {
            tree.write(tempId, data)
            val finalId = tree.rename(tempId, fileName)
            children[fileName] = DocumentEntry(finalId, fileName, isDirectory = false)
        } catch (e: IOException) {
            tree.delete(tempId)
            throw e
        }
    }

    override fun load(type: FileType, name: String): ByteArray {
        val path = RepositoryLayout.path(type, name)
        val entry = file(path) ?: throw FileNotFoundException(path)
        return tree.read(entry.id)
    }

    override fun exists(type: FileType, name: String): Boolean =
        file(RepositoryLayout.path(type, name)) != null

    override fun list(type: FileType): List<String> {
        val folderId =
            directory(RepositoryLayout.directory(type), create = false) ?: return emptyList()
        val folders =
            if (type == FileType.PACK)
                listing(folderId).values.filter { it.isDirectory }.map { it.id }
            else listOf(folderId)
        return folders.flatMap { id ->
            listing(id)
                .values
                .filter { !it.isDirectory && !it.name.startsWith(TEMP_PREFIX) }
                .map { it.name }
        }
    }

    override fun delete(type: FileType, name: String) {
        val (folder, fileName) = split(RepositoryLayout.path(type, name))
        val folderId = directory(folder, create = false) ?: return
        val entry = listing(folderId).remove(fileName) ?: return
        tree.delete(entry.id)
    }

    private fun file(path: String): DocumentEntry? {
        val (folder, fileName) = split(path)
        val folderId = directory(folder, create = false) ?: return null
        return listing(folderId)[fileName]?.takeIf { !it.isDirectory }
    }

    private fun directory(path: String, create: Boolean): String? =
        directories[path] ?: findDirectory(path, create)?.also { directories[path] = it }

    private fun findDirectory(path: String, create: Boolean): String? {
        val (parent, name) = split(path)
        val parentId = directory(parent, create) ?: return null
        val children = listing(parentId)
        val existing = children[name]
        return when {
            existing != null && existing.isDirectory -> existing.id
            existing != null -> throw IOException("$path is a file, not a folder")
            !create -> null
            else ->
                tree.createDirectory(parentId, name).also {
                    children[name] = DocumentEntry(it, name, isDirectory = true)
                }
        }
    }

    private fun listing(directoryId: String): MutableMap<String, DocumentEntry> =
        listings.getOrPut(directoryId) {
            tree.children(directoryId).associateByTo(HashMap()) { it.name }
        }

    private fun split(path: String): Pair<String, String> {
        val slash = path.lastIndexOf('/')
        return if (slash < 0) "" to path else path.substring(0, slash) to path.substring(slash + 1)
    }

    private companion object {
        const val TEMP_PREFIX = ".tmp-"
    }
}
