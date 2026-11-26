// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

import java.time.Instant

data class BackupOverview(
    val location: BackupLocation?,
    val lastBackup: LastBackup?,
    val categories: List<CategoryStatus>,
    val recoveryPhraseSaved: Boolean,
    val lastVerified: Instant?,
    val schedule: BackupSchedule,
    val connectedApps: Int,
) {

    companion object {
        fun notSetUp(): BackupOverview =
            BackupOverview(
                location = null,
                lastBackup = null,
                categories =
                    BackupCategory.entries.map { CategoryStatus(it, items = null, bytes = null) },
                recoveryPhraseSaved = false,
                lastVerified = null,
                schedule = BackupSchedule.DAILY_WHILE_CHARGING,
                connectedApps = 0,
            )
    }
}
