// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

object BackupContract {
    const val VERSION = 1
    const val ACTION_AGENT = "org.prauga.cloud.sdk.AGENT"
    const val ACTION_PROVIDER = "org.prauga.cloud.sdk.PROVIDER"
    const val ACTION_TRUST_PROVIDER = "org.prauga.cloud.sdk.TRUST_PROVIDER"
    const val META_CONFIG = "org.prauga.cloud.sdk.CONFIG"
    const val META_TRUSTED_PROVIDERS = "org.prauga.cloud.sdk.TRUSTED_PROVIDERS"
    const val TYPE_APP_DATA = "app-data"
    const val TYPE_CREDENTIALS = "credentials"
    const val CAPABILITY_BACKUP = "BACKUP"
}
