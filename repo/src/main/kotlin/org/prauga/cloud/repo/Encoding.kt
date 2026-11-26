// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import java.security.MessageDigest
import java.util.Base64
import kotlinx.serialization.json.Json

internal val ResticJson = Json { ignoreUnknownKeys = true }

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun ByteArray.base64(): String = Base64.getEncoder().encodeToString(this)

internal fun String.decodeBase64(): ByteArray = Base64.getDecoder().decode(this)

internal fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)
