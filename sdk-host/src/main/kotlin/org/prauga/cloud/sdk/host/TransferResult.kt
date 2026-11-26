// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import org.prauga.cloud.sdk.ResultCode

data class TransferResult(val code: ResultCode, val reason: String?, val bytes: Long)
