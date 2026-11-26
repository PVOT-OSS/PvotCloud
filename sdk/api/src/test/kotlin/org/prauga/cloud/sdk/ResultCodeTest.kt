// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class ResultCodeTest {

    @Test
    fun codesRoundTripAndUnknownCodesAreInternal() {
        ResultCode.entries.forEach { assertThat(ResultCode.of(it.code)).isEqualTo(it) }
        assertThat(ResultCode.of(99)).isEqualTo(ResultCode.INTERNAL)
    }

    @Test
    fun unknownKindsAreCustom() {
        assertThat(DataKind.of("CREDENTIALS")).isEqualTo(DataKind.CREDENTIALS)
        assertThat(DataKind.of("RECORDS")).isEqualTo(DataKind.CUSTOM)
        assertThat(DataKind.of(null)).isEqualTo(DataKind.CUSTOM)
    }
}
