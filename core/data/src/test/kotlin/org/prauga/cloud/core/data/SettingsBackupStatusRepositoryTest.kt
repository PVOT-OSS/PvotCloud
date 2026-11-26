// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.prauga.cloud.core.datastore.BackupSettingsDataSource
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.SavedLocation

class SettingsBackupStatusRepositoryTest {

    @get:Rule val folder = TemporaryFolder()

    @Test
    fun overviewFollowsTheSavedSettings() =
        runTest(UnconfinedTestDispatcher()) {
            val dataSource =
                BackupSettingsDataSource(
                    PreferenceDataStoreFactory.create(scope = backgroundScope) {
                        folder.root.resolve("settings.preferences_pb")
                    }
                )
            val repository = SettingsBackupStatusRepository(dataSource)

            val before = repository.overview.first()
            assertNull(before.location)
            assertFalse(before.recoveryPhraseSaved)
            assertEquals(BackupCategory.entries, before.categories.map { it.category })

            dataSource.setLocation(SavedLocation("content://tree/1", "SanDisk"))
            dataSource.setCategories(setOf(BackupCategory.CONTACTS, BackupCategory.PHOTOS))
            dataSource.setPhraseConfirmed(true)

            val after = repository.overview.first()
            assertEquals("SanDisk", after.location?.name)
            assertTrue(after.recoveryPhraseSaved)
            assertEquals(
                listOf(BackupCategory.PHOTOS, BackupCategory.CONTACTS),
                after.categories.map { it.category },
            )
        }
}
