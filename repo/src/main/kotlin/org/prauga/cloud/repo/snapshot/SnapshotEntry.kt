// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.snapshot

import org.prauga.cloud.repo.Id

data class SnapshotEntry(val id: Id, val snapshot: Snapshot)
