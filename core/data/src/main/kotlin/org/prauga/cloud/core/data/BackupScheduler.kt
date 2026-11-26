// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import kotlinx.coroutines.flow.Flow
import org.prauga.cloud.core.model.RunProgress

interface BackupScheduler {
    val running: Flow<RunProgress?>

    fun backUpNow()
}
