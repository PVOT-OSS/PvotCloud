// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.pvot.quality.spotless)
}

extra.apply {
    val versionMajor = 1
    val versionMinor = 0
    val versionPatch = 0
    set("versionCode", versionMajor * 10000 + versionMinor * 100 + versionPatch)
    set("versionName", "$versionMajor.$versionMinor.$versionPatch")
}
