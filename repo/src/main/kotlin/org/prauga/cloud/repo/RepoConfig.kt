// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepoConfig(
    val version: Int,
    val id: String,
    @SerialName("chunker_polynomial") val chunkerPolynomial: String,
)
