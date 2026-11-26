// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.pvot.android.library)
    alias(libs.plugins.pvot.hilt)
}

android { namespace = "org.prauga.cloud.core.data" }

dependencies {
    api(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:keys"))
    implementation(project(":storage"))
    implementation(project(":sources"))
    implementation(project(":sdk-host"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertk)
    testImplementation(libs.androidx.datastore.preferences)
}
