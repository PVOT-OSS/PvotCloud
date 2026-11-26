// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins { alias(libs.plugins.pvot.android.library) }

android { namespace = "org.prauga.cloud.sdk.agent" }

dependencies {
    api(project(":sdk:api"))
    testImplementation(libs.assertk)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.assertk)
}
