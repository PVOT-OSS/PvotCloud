// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialsFormatTest {

    private val credentials =
        listOf(
            Credential(
                "1",
                "Mail",
                "me@example.org",
                "p\"ss\nw0rd",
                uri = "https://mail.example.org",
            ),
            Credential("2", "Wi-Fi ☃", "", "hunter2", notes = "kitchen router"),
        )

    @Test
    fun readsBackWhatItWrote() {
        val out = ByteArrayOutputStream()

        CredentialsFormat.write(credentials, out)

        assertThat(CredentialsFormat.read(ByteArrayInputStream(out.toByteArray())))
            .isEqualTo(credentials)
    }

    @Test
    fun rejectsOtherDocuments() {
        assertFailure {
            CredentialsFormat.read(ByteArrayInputStream("{\"format\":\"x\"}".toByteArray()))
        }
            .isInstanceOf<IOException>()
        assertFailure { CredentialsFormat.read(ByteArrayInputStream("not json".toByteArray())) }
            .isInstanceOf<IOException>()
    }

    @Test
    fun toStringHidesTheSecret() {
        assertThat(credentials.first().toString()).doesNotContain("w0rd")
    }
}
