// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.index

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IndexJson(val supersedes: List<String> = emptyList(), val packs: List<Pack>) {

    @Serializable data class Pack(val id: String, val blobs: List<Blob>)

    @Serializable
    data class Blob(
        val id: String,
        val type: String,
        val offset: Long,
        val length: Int,
        @SerialName("uncompressed_length") val uncompressedLength: Int? = null,
    )
}
