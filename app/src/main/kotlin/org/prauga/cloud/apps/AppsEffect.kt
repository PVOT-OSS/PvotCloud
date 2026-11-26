// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.apps

sealed interface AppsEffect {
    data class AskApproval(val request: ApprovalRequest) : AppsEffect
}
