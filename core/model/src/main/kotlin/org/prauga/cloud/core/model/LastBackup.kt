// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

import java.time.Instant

data class LastBackup(val finishedAt: Instant, val outcome: BackupOutcome)
