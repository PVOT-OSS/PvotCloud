// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

sealed interface RunResult {
    data class Success(val files: Int, val bytes: Long) : RunResult

    data class Failure(val reason: BackupFailure) : RunResult
}
