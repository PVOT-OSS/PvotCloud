// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import android.content.Context
import org.prauga.cloud.sdk.agent.BackupAgentConfig
import org.prauga.cloud.sdk.agent.BackupAgentConfigProvider
import org.prauga.cloud.sdk.agent.backupAgent

class TestAgentConfig : BackupAgentConfigProvider {
    override fun backupAgentConfig(context: Context): BackupAgentConfig = backupAgent {
        everything()
        needsRestart = false
    }
}
