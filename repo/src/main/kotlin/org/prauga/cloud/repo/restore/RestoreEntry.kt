// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.restore

import org.prauga.cloud.repo.tree.Node

data class RestoreEntry(val path: String, val node: Node)
