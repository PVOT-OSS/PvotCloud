// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.cloud.core.model.BackupCategory

class InMemoryBackupStatusRepositoryTest {

    @Test
    fun startsNotSetUp() = runTest {
        val overview = InMemoryBackupStatusRepository().overview.first()

        assertNull(overview.location)
        assertNull(overview.lastBackup)
        assertFalse(overview.recoveryPhraseSaved)
        assertEquals(BackupCategory.entries, overview.categories.map { it.category })
        assertTrue(overview.categories.all { it.items == null && it.bytes == null })
    }
}
