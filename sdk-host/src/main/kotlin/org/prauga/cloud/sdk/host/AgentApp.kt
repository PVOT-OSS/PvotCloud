// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

data class AgentApp(
    val packageName: String,
    val label: String,
    val certificates: Set<String>,
    val versionCode: Long,
)
