// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.apps

sealed interface AppsEvent {
    data class SetIncluded(val packageName: String, val included: Boolean) : AppsEvent

    data class Restore(val packageName: String) : AppsEvent

    data class Approved(val request: ApprovalRequest) : AppsEvent

    data object Refresh : AppsEvent

    data object DismissMessage : AppsEvent
}
