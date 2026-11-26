// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceEntry
import org.prauga.cloud.repo.source.SourceFile

internal class FileTree(private val dir: File, override val name: String = dir.name) :
    SourceDirectory {
    override val modTime: OffsetDateTime = time(dir)

    override fun children(): List<SourceEntry> =
        dir.listFiles()
            .orEmpty()
            .sortedBy { it.name }
            .map { file ->
                if (file.isDirectory) FileTree(file)
                else
                    object : SourceFile {
                        override val name: String = file.name
                        override val modTime: OffsetDateTime = time(file)

                        override fun open(): InputStream = file.inputStream()
                    }
            }

    private companion object {
        fun time(file: File): OffsetDateTime =
            Instant.ofEpochMilli(file.lastModified()).atOffset(ZoneOffset.UTC)
    }
}
