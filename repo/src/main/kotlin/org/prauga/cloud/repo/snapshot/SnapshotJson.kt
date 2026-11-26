// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.snapshot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SnapshotJson(
    val time: String,
    val parent: String? = null,
    val tree: String,
    val paths: List<String> = emptyList(),
    val hostname: String? = null,
    val username: String? = null,
    val uid: Long = 0,
    val gid: Long = 0,
    val excludes: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val original: String? = null,
    @SerialName("program_version") val programVersion: String? = null,
)
