// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

enum class FileType(internal val directory: String) {
    CONFIG(""),
    KEY("keys"),
    PACK("data"),
    INDEX("index"),
    SNAPSHOT("snapshots"),
    LOCK("locks"),
}
