# SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
# SPDX-License-Identifier: Apache-2.0

# The agent creates the app's configuration from the class named in the manifest.
-keep class * implements org.prauga.cloud.sdk.agent.BackupAgentConfigProvider {
    public <init>();
}
