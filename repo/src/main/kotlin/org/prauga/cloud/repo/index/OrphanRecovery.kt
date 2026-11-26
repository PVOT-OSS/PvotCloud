// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.index

import org.prauga.cloud.repo.Id

data class OrphanRecovery(val recovered: List<Id>, val damaged: List<Id>)
