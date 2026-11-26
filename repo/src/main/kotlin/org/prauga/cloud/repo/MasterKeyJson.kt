// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import kotlinx.serialization.Serializable

@Serializable
internal data class MasterKeyJson(val mac: Mac, val encrypt: String) {

    @Serializable data class Mac(val k: String, val r: String)
}
