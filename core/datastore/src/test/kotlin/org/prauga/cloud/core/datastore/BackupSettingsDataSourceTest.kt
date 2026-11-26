// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupOutcome
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.CategoryCount
import org.prauga.cloud.core.model.LastBackup
import org.prauga.cloud.core.model.SavedLocation

@OptIn(ExperimentalCoroutinesApi::class)
class BackupSettingsDataSourceTest {

    @get:Rule val folder = TemporaryFolder()

    private fun testSubject(scope: TestScope) =
        BackupSettingsDataSource(
            PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                folder.root.resolve("settings.preferences_pb")
            }
        )

    @Test
    fun startsWithDefaults() =
        runTest(UnconfinedTestDispatcher()) {
            assertThat(testSubject(this).settings.first()).isEqualTo(BackupSettings())
        }

    @Test
    fun savesWhatTheUserChose() =
        runTest(UnconfinedTestDispatcher()) {
            val testSubject = testSubject(this)
            val location =
                SavedLocation(
                    "content://com.android.externalstorage.documents/tree/1234",
                    "SanDisk",
                )

            testSubject.setLocation(location)
            testSubject.setCategories(setOf(BackupCategory.PHOTOS, BackupCategory.CONTACTS))
            testSubject.setPhraseConfirmed(true)

            assertThat(testSubject.settings.first())
                .isEqualTo(
                    BackupSettings(
                        location,
                        setOf(BackupCategory.PHOTOS, BackupCategory.CONTACTS),
                        phraseConfirmed = true,
                    )
                )
        }

    @Test
    fun remembersTheLastBackup() =
        runTest(UnconfinedTestDispatcher()) {
            val testSubject = testSubject(this)
            val first = Instant.parse("2026-11-27T10:00:00Z")
            val failed = first.plusSeconds(3600)
            val counts = mapOf(BackupCategory.PHOTOS to CategoryCount(9013, 41_000_000_000L))

            testSubject.recordSuccess(first, counts)
            val succeeded = testSubject.settings.first()
            testSubject.recordFailure(failed)
            val afterFailure = testSubject.settings.first()

            assertThat(succeeded.lastBackup).isEqualTo(LastBackup(first, BackupOutcome.SUCCESS))
            assertThat(succeeded.counts).isEqualTo(counts)
            assertThat(afterFailure.lastBackup).isEqualTo(LastBackup(failed, BackupOutcome.FAILED))
            assertThat(afterFailure.counts).isEqualTo(counts)
        }
}
