// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.prauga.cloud.core.datastore.BackupSettingsDataSource
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupOutcome
import org.prauga.cloud.core.model.BackupOverview
import org.prauga.cloud.core.model.CategoryCount
import org.prauga.cloud.core.model.LastBackup
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
            val testSubject = SettingsBackupStatusRepository(dataSource)
            val finished = Instant.parse("2026-11-27T10:00:00Z")

            val before = testSubject.overview.first()
            dataSource.setLocation(SavedLocation("content://tree/1", "SanDisk"))
            dataSource.setCategories(setOf(BackupCategory.CONTACTS, BackupCategory.PHOTOS))
            dataSource.setPhraseConfirmed(true)
            val after = testSubject.overview.first()
            dataSource.recordSuccess(
                finished,
                mapOf(BackupCategory.PHOTOS to CategoryCount(12, 3456)),
            )
            val backedUp = testSubject.overview.first()

            assertThat(before.location).isNull()
            assertThat(before.recoveryPhraseSaved).isFalse()
            assertThat(before.categories.map { it.category }).isEqualTo(BackupCategory.entries)
            assertThat(after.location?.name).isEqualTo("SanDisk")
            assertThat(after.recoveryPhraseSaved).isTrue()
            assertThat(after.categories.map { it.category })
                .isEqualTo(listOf(BackupCategory.PHOTOS, BackupCategory.CONTACTS))
            assertThat(backedUp)
                .prop(BackupOverview::lastBackup)
                .isEqualTo(LastBackup(finished, BackupOutcome.SUCCESS))
            assertThat(backedUp.itemsOf(BackupCategory.PHOTOS)).isEqualTo(12)
            assertThat(backedUp.itemsOf(BackupCategory.CONTACTS)).isNull()
        }

    private fun BackupOverview.itemsOf(category: BackupCategory): Int? =
        categories.first { it.category == category }.items
}
