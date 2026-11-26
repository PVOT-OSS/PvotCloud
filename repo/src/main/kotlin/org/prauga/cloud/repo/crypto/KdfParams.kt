// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.crypto

data class KdfParams(val n: Int, val r: Int, val p: Int) {

    init {
        require(n > 1 && (n and (n - 1)) == 0) { "N must be a power of two" }
        require(r >= 1 && p >= 1) { "r and p must be positive" }
    }

    companion object {
        val DEFAULT = KdfParams(n = 32768, r = 8, p = 1)
    }
}
