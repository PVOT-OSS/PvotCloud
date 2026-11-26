// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins {
    alias(libs.plugins.pvot.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.zstd.jni)
}
