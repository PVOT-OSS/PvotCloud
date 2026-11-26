// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

import android.os.Bundle

data class DataTypeInfo(val name: String, val kind: DataKind, val estimatedBytes: Long) {

    fun toBundle(): Bundle =
        Bundle().apply {
            putString(NAME, name)
            putString(KIND, kind.name)
            putLong(ESTIMATED_BYTES, estimatedBytes)
        }

    companion object {
        private const val NAME = "name"
        private const val KIND = "kind"
        private const val ESTIMATED_BYTES = "estimatedBytes"

        fun fromBundle(bundle: Bundle): DataTypeInfo? {
            val name = bundle.getString(NAME) ?: return null
            return DataTypeInfo(
                name,
                DataKind.of(bundle.getString(KIND)),
                bundle.getLong(ESTIMATED_BYTES),
            )
        }
    }
}
