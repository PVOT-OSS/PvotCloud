// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.Context

fun interface BackupAgentConfigProvider {
    fun backupAgentConfig(context: Context): BackupAgentConfig
}
