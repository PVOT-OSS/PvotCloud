// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.tree

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NodeJson(
    val name: String,
    val type: String,
    val mode: Long = 0,
    val mtime: String = GO_ZERO_TIME,
    val atime: String = GO_ZERO_TIME,
    val ctime: String = GO_ZERO_TIME,
    val uid: Long = 0,
    val gid: Long = 0,
    val user: String? = null,
    val group: String? = null,
    val inode: Long = 0,
    @SerialName("device_id") val deviceId: Long = 0,
    val size: Long = 0,
    val links: Long = 0,
    val linktarget: String? = null,
    val device: Long = 0,
    val content: List<String>? = null,
    val subtree: String? = null,
) {

    @Serializable internal data class Tree(val nodes: List<NodeJson> = emptyList())

    companion object {
        const val GO_ZERO_TIME = "0001-01-01T00:00:00Z"
    }
}
