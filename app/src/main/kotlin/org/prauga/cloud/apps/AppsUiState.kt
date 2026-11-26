// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.apps

import org.prauga.cloud.core.model.AppRestoreResult
import org.prauga.cloud.core.model.ConnectedApp

data class AppsUiState(
    val loading: Boolean = true,
    val apps: List<ConnectedApp> = emptyList(),
    val restoring: String? = null,
    val restored: RestoreMessage? = null,
)

data class RestoreMessage(val label: String, val result: AppRestoreResult)
