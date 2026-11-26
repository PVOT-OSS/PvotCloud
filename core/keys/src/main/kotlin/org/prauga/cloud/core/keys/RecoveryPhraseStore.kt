// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.keys

interface RecoveryPhraseStore {
    suspend fun save(words: List<String>)

    suspend fun load(): List<String>?

    suspend fun exists(): Boolean
}
