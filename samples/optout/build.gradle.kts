// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins { alias(libs.plugins.pvot.android.application) }

android {
    namespace = "org.prauga.cloud.sample.optout"

    defaultConfig {
        applicationId = "org.prauga.cloud.sample.optout"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes { release { optimization { enable = true } } }
}

dependencies { implementation(project(":sdk:agent")) }
