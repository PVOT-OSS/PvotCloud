// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

internal sealed interface AppDataSpec {
    data object Everything : AppDataSpec

    data class Selected(
        val databases: List<String>,
        val sharedPreferences: List<String>,
        val files: List<String>,
    ) : AppDataSpec
}
