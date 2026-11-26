// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.SavedLocation

class BackupSettingsDataSourceTest {

    @get:Rule val folder = TemporaryFolder()

    private fun dataSource(scope: TestScope) =
        BackupSettingsDataSource(
            PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                folder.root.resolve("settings.preferences_pb")
            }
        )

    @Test
    fun startsWithDefaults() =
        runTest(UnconfinedTestDispatcher()) {
            assertEquals(BackupSettings(), dataSource(this).settings.first())
        }

    @Test
    fun savesWhatTheUserChose() =
        runTest(UnconfinedTestDispatcher()) {
            val dataSource = dataSource(this)
            val location =
                SavedLocation(
                    "content://com.android.externalstorage.documents/tree/1234",
                    "SanDisk",
                )

            dataSource.setLocation(location)
            dataSource.setCategories(setOf(BackupCategory.PHOTOS, BackupCategory.CONTACTS))
            dataSource.setPhraseConfirmed(true)

            assertEquals(
                BackupSettings(
                    location,
                    setOf(BackupCategory.PHOTOS, BackupCategory.CONTACTS),
                    phraseConfirmed = true,
                ),
                dataSource.settings.first(),
            )
        }
}
