// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.backup

import org.prauga.cloud.repo.Id

data class BackupResult(
    val snapshotId: Id,
    val tree: Id,
    val files: Int,
    val directories: Int,
    val bytes: Long,
    val newBlobs: Int,
    val newPacks: Int,
)
