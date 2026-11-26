// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.source

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.OffsetDateTime

private val TIME: OffsetDateTime = OffsetDateTime.parse("2026-11-26T12:00:00Z")

class MemoryFile(
    override val name: String,
    val data: ByteArray,
    override val modTime: OffsetDateTime = TIME,
) : SourceFile {
    override fun open(): InputStream = ByteArrayInputStream(data)
}

class MemoryDirectory(
    override val name: String,
    private val entries: List<SourceEntry>,
    override val modTime: OffsetDateTime = TIME,
) : SourceDirectory {
    override fun children(): List<SourceEntry> = entries
}

class MemorySymlink(
    override val name: String,
    override val target: String,
    override val modTime: OffsetDateTime = TIME,
) : SourceSymlink
