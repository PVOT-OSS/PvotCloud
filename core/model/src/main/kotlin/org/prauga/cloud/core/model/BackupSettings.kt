// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

data class BackupSettings(
    val location: SavedLocation? = null,
    val categories: Set<BackupCategory> = BackupCategory.entries.toSet(),
    val phraseConfirmed: Boolean = false,
)
