// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.check

data class CheckResult(
    val snapshots: Int,
    val trees: Int,
    val packs: Int,
    val problems: List<CheckProblem>,
) {
    val ok: Boolean
        get() = problems.isEmpty()
}
