// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

enum class DataKind {
    APP_DATA,
    CREDENTIALS,
    CUSTOM;

    companion object {
        fun of(name: String?): DataKind = entries.firstOrNull { it.name == name } ?: CUSTOM
    }
}
