// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins { alias(libs.plugins.pvot.android.library) }

android { namespace = "org.prauga.cloud.storage" }

dependencies {
    api(project(":repo"))
    testImplementation(libs.zstd.jni)
}
