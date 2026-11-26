// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class KeyFile(
    val created: String,
    val username: String,
    val hostname: String,
    val kdf: String,
    @SerialName("N") val n: Int,
    val r: Int,
    val p: Int,
    val salt: String,
    val data: String,
)
