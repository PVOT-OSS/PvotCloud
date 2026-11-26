// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.walk
import kotlin.io.path.writeBytes

class FileSystemBackend(private val root: Path) : Backend {

    override fun save(type: FileType, name: String, data: ByteArray) {
        val target = root.resolve(RepositoryLayout.path(type, name))
        Files.createDirectories(target.parent)
        val temp = Files.createTempFile(target.parent, ".tmp-", "")
        temp.writeBytes(data)
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
    }

    override fun load(type: FileType, name: String): ByteArray =
        root.resolve(RepositoryLayout.path(type, name)).readBytes()

    override fun exists(type: FileType, name: String): Boolean =
        root.resolve(RepositoryLayout.path(type, name)).exists()

    override fun list(type: FileType): List<String> {
        val dir = root.resolve(type.directory)
        if (!dir.isDirectory()) return emptyList()
        return dir.walk()
            .filter { it.isRegularFile() && !it.name.startsWith(".tmp-") }
            .map { it.name }
            .toList()
    }

    override fun delete(type: FileType, name: String) {
        Files.deleteIfExists(root.resolve(RepositoryLayout.path(type, name)))
    }
}
