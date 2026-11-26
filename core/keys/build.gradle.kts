// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins {
    alias(libs.plugins.pvot.android.library)
    alias(libs.plugins.pvot.hilt)
}

android { namespace = "org.prauga.cloud.core.keys" }

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.bip39)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
