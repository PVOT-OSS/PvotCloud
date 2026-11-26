// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.source

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private fun modTime(path: Path): OffsetDateTime =
    Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant().atOffset(ZoneOffset.UTC)

class LocalDirectory(private val path: Path) : SourceDirectory {
    override val name: String = path.name
    override val modTime: OffsetDateTime = modTime(path)

    override fun children(): List<SourceEntry> =
        path.listDirectoryEntries().map {
            when {
                it.isSymbolicLink() -> LocalSymlink(it)
                it.isDirectory() -> LocalDirectory(it)
                else -> LocalFile(it)
            }
        }
}

class LocalFile(private val path: Path) : SourceFile {
    override val name: String = path.name
    override val modTime: OffsetDateTime = modTime(path)

    override fun open(): InputStream = path.inputStream()
}

class LocalSymlink(path: Path) : SourceSymlink {
    override val name: String = path.name
    override val modTime: OffsetDateTime = modTime(path)
    override val target: String = Files.readSymbolicLink(path).toString()
}
