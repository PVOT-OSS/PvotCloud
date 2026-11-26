// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins { alias(libs.plugins.pvot.android.application) }

android {
    namespace = "org.prauga.cloud.sample.ledger"

    defaultConfig {
        applicationId = "org.prauga.cloud.sample.ledger"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes { release { optimization { enable = true } } }
}

dependencies { implementation(project(":sdk:agent")) }
