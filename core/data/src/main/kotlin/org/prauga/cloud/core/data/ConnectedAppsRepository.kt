// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import kotlinx.coroutines.flow.Flow
import org.prauga.cloud.core.model.AppRestoreResult
import org.prauga.cloud.core.model.ConnectedApp

interface ConnectedAppsRepository {
    val apps: Flow<List<ConnectedApp>>

    fun refresh()

    suspend fun isApproved(packageName: String): Boolean

    suspend fun include(packageName: String)

    suspend fun exclude(packageName: String)

    suspend fun restore(packageName: String): AppRestoreResult
}
