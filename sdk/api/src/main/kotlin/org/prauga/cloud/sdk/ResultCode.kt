// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

// The codes are the values sent over Binder and never change.
@Suppress("MagicNumber")
enum class ResultCode(val code: Int) {
    OK(0),
    SKIPPED(1),
    NOT_CONSENTED(2),
    UNTRUSTED_PEER(3),
    SCHEMA_TOO_NEW(4),
    BUSY(5),
    IO(6),
    INTERNAL(7);

    companion object {
        fun of(code: Int): ResultCode = entries.firstOrNull { it.code == code } ?: INTERNAL
    }
}
