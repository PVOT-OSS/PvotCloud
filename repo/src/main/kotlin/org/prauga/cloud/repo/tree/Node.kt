// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.tree

import java.time.OffsetDateTime
import org.prauga.cloud.repo.Id

data class Node(
    val name: String,
    val type: NodeType,
    val modTime: OffsetDateTime,
    val accessTime: OffsetDateTime = modTime,
    val changeTime: OffsetDateTime = modTime,
    val mode: Long = 0,
    val uid: Long = 0,
    val gid: Long = 0,
    val user: String? = null,
    val group: String? = null,
    val inode: Long = 0,
    val deviceId: Long = 0,
    val size: Long = 0,
    val links: Long = 0,
    val linkTarget: String? = null,
    val device: Long = 0,
    val content: List<Id>? = null,
    val subtree: Id? = null,
) {

    companion object {
        const val MODE_DIR = 1L shl 31
        const val MODE_SYMLINK = 1L shl 27
    }
}
