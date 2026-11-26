// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import kotlinx.coroutines.flow.Flow
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.SavedLocation

interface BackupSetupRepository {
    val settings: Flow<BackupSettings>

    suspend fun chooseLocation(treeUri: String): SavedLocation?

    suspend fun setCategories(categories: Set<BackupCategory>)

    suspend fun confirmPhrase()
}
