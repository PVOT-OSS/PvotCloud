// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import org.prauga.cloud.sdk.ResultCode

data class AppResult(
    val packageName: String,
    val code: ResultCode,
    val detail: String? = null,
    val bytes: Long = 0,
)
