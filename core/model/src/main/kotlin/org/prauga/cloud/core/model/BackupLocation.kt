// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

data class BackupLocation(
    val name: String,
    val usedBytes: Long? = null,
    val totalBytes: Long? = null,
)
