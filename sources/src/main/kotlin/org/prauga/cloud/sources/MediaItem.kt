// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sources

import java.time.Instant

data class MediaItem(
    val id: Long,
    val relativePath: String,
    val name: String,
    val size: Long,
    val modified: Instant,
    val video: Boolean = false,
)
