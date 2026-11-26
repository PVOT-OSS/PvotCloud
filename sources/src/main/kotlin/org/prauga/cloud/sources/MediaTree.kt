// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sources

import java.io.InputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceEntry
import org.prauga.cloud.repo.source.SourceFile

object MediaTree {

    fun build(
        name: String,
        items: List<MediaItem>,
        open: (MediaItem) -> InputStream,
    ): SourceDirectory {
        val root = Folder(name)
        for (item in items) {
            val folder =
                item.relativePath
                    .split('/')
                    .filter { it.isNotEmpty() }
                    .fold(root) { parent, segment ->
                        parent.folders.getOrPut(segment) { Folder(segment) }
                    }
            folder.files += item
        }
        return root.toSource(open)
    }

    private class Folder(val name: String) {
        val folders = LinkedHashMap<String, Folder>()
        val files = mutableListOf<MediaItem>()

        fun toSource(open: (MediaItem) -> InputStream): SourceDirectory {
            val newest = files.maxOfOrNull { it.modified }
            val time = newest?.atOffset(ZoneOffset.UTC) ?: EPOCH
            val children = buildList {
                val taken = folders.keys.toMutableSet()
                folders.values.forEach { add(it.toSource(open)) }
                files
                    .sortedBy { it.id }
                    .forEach { item -> add(MediaFile(uniqueName(item.name, taken), item, open)) }
            }
            return Directory(name, time, children)
        }
    }

    private class Directory(
        override val name: String,
        override val modTime: OffsetDateTime,
        private val entries: List<SourceEntry>,
    ) : SourceDirectory {
        override fun children(): List<SourceEntry> = entries
    }

    private class MediaFile(
        override val name: String,
        private val item: MediaItem,
        private val opener: (MediaItem) -> InputStream,
    ) : SourceFile {
        override val modTime: OffsetDateTime = item.modified.atOffset(ZoneOffset.UTC)

        override fun open(): InputStream = opener(item)
    }

    private fun uniqueName(name: String, taken: MutableSet<String>): String {
        if (taken.add(name)) return name
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        var n = 2
        while (true) {
            val candidate = "${name.substring(0, dot)} ($n)${name.substring(dot)}"
            if (taken.add(candidate)) return candidate
            n++
        }
    }

    private val EPOCH: OffsetDateTime = Instant.EPOCH.atOffset(ZoneOffset.UTC)
}
