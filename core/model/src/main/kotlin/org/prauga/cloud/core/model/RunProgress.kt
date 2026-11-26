// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

data class RunProgress(val files: Int, val totalFiles: Int, val bytes: Long, val totalBytes: Long)
