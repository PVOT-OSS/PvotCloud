// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.cloud.repo.crypto.KdfParams

class RepositoryTest {

    private val params = KdfParams(n = 1024, r = 8, p = 1)

    private fun create(backend: Backend) =
        Repository.create(backend, PASSWORD, hostname = "phone", username = "user", params = params)

    @Test
    fun createdRepositoryOpensWithThePassword() {
        val backend = InMemoryBackend()
        val created = create(backend)

        val opened = Repository.open(backend, PASSWORD)

        assertEquals(created.config, opened.config)
        assertEquals(Repository.VERSION, opened.config.version)
        assertEquals(64, opened.config.id.length)
        assertEquals(53, opened.chunkerPolynomial.degree)
    }

    @Test(expected = RepositoryException.WrongPassword::class)
    fun wrongPasswordIsRejected() {
        val backend = InMemoryBackend()
        create(backend)

        Repository.open(backend, "not the password")
    }

    @Test(expected = RepositoryException.AlreadyExists::class)
    fun refusesToOverwriteARepository() {
        val backend = InMemoryBackend()
        create(backend)

        create(backend)
    }

    private companion object {
        const val PASSWORD =
            "abandon ability able about above absent absorb abstract absurd abuse access accident"
    }
}
