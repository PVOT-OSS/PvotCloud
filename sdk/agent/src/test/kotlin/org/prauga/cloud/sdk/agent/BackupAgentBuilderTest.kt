// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.Test

class BackupAgentBuilderTest {

    @Test
    fun declaredDataBecomesASelection() {
        val testSubject = backupAgent {
            schemaVersion = 3
            database("ledger.db")
            dataStore("settings")
            sharedPreferences("prefs")
            files("/presets/")
            reEnrolOnRestore("Fingerprint unlock")
        }

        assertThat(testSubject.schemaVersion).isEqualTo(3)
        assertThat(testSubject.appData)
            .isEqualTo(
                AppDataSpec.Selected(
                    databases = listOf("ledger.db"),
                    sharedPreferences = listOf("prefs"),
                    files = listOf("datastore/settings.preferences_pb", "presets"),
                )
            )
        assertThat(testSubject.reEnrolOnRestore).containsExactly("Fingerprint unlock")
    }

    @Test
    fun onlyCustomTypesMeansNoAppData() {
        val testSubject = backupAgent {
            custom("accounts", export = {}, import = { _, _ -> })
            credentials(export = { emptyList() }, import = {})
        }

        assertThat(testSubject.appData).isNull()
        assertThat(testSubject.customTypes.keys.toList()).containsExactly("accounts")
        assertThat(testSubject.credentials).isNotNull()
    }

    @Test
    fun everythingWins() {
        val testSubject = backupAgent {
            everything()
            database("ledger.db")
        }

        assertThat(testSubject.appData).isEqualTo(AppDataSpec.Everything)
    }

    @Test
    fun reservedNamesAndBadSchemasAreRejected() {
        assertFailure { backupAgent { custom("credentials", export = {}, import = { _, _ -> }) } }
            .isInstanceOf<IllegalArgumentException>()
        assertFailure { backupAgent { schemaVersion = 0 } }.isInstanceOf<IllegalArgumentException>()
    }
}
