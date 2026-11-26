// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.prauga.cloud.core.datastore.BackupSettingsDataSource
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupLocation
import org.prauga.cloud.core.model.BackupOverview
import org.prauga.cloud.core.model.CategoryStatus

class SettingsBackupStatusRepository @Inject constructor(dataSource: BackupSettingsDataSource) :
    BackupStatusRepository {

    override val overview: Flow<BackupOverview> =
        dataSource.settings.map { settings ->
            BackupOverview.notSetUp()
                .copy(
                    location = settings.location?.let { BackupLocation(it.name) },
                    categories =
                        BackupCategory.entries
                            .filter { it in settings.categories }
                            .map {
                                CategoryStatus(
                                    it,
                                    settings.counts[it]?.items,
                                    settings.counts[it]?.bytes,
                                )
                            },
                    recoveryPhraseSaved = settings.phraseConfirmed,
                    lastBackup = settings.lastBackup,
                )
        }
}
