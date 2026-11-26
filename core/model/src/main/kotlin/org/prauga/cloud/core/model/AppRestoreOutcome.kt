// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.model

enum class AppRestoreOutcome {
    RESTORED,
    NOT_APPROVED,
    NO_BACKUP,
    NOT_SET_UP,
    FAILED,
}
