// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.prauga.cloud.core.model.BackupOverview

class InMemoryBackupStatusRepository @Inject constructor() : BackupStatusRepository {

    private val state = MutableStateFlow(BackupOverview.notSetUp())

    override val overview: Flow<BackupOverview> = state.asStateFlow()
}
