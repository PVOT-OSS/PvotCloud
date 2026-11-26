// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.snapshot

import java.time.OffsetDateTime
import org.prauga.cloud.repo.Id

data class Snapshot(
    val time: OffsetDateTime,
    val tree: Id,
    val paths: List<String>,
    val hostname: String? = null,
    val username: String? = null,
    val tags: List<String> = emptyList(),
    val parent: Id? = null,
    val original: Id? = null,
    val programVersion: String? = null,
)
