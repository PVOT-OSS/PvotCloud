// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

data class BackupSettings(
    val location: SavedLocation? = null,
    val categories: Set<BackupCategory> = BackupCategory.entries.toSet(),
    val phraseConfirmed: Boolean = false,
    val lastBackup: LastBackup? = null,
    val counts: Map<BackupCategory, CategoryCount> = emptyMap(),
    val includedApps: Set<IncludedApp> = emptySet(),
    val appRecords: Map<String, AppRecord> = emptyMap(),
)
