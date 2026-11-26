// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.apps

data class ApprovalRequest(val packageName: String, val then: Then) {
    enum class Then {
        INCLUDE,
        RESTORE,
    }
}
