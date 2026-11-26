// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.chunker

import org.junit.Assert.assertEquals
import org.junit.Test

class GoRandomTest {

    @Test
    fun matchesGoMathRandForSeedOne() {
        val random = GoRandom(1)

        assertEquals(5577006791947779410L, random.int63())
        assertEquals(8674665223082153551L, random.int63())
    }
}
