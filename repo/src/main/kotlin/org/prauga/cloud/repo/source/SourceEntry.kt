// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.source

import java.io.InputStream
import java.time.OffsetDateTime

sealed interface SourceEntry {
    val name: String
    val modTime: OffsetDateTime
}

interface SourceFile : SourceEntry {
    fun open(): InputStream
}

interface SourceDirectory : SourceEntry {
    fun children(): List<SourceEntry>
}

interface SourceSymlink : SourceEntry {
    val target: String
}
