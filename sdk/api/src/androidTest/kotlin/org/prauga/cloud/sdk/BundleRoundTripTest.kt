// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

import android.os.Bundle
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundleRoundTripTest {

    @Test
    fun agentInfoSurvivesAParcel() {
        val testSubject =
            AgentInfo(
                contractVersion = BackupContract.VERSION,
                schemaVersion = 3,
                types =
                    listOf(
                        DataTypeInfo(BackupContract.TYPE_APP_DATA, DataKind.APP_DATA, 4096),
                        DataTypeInfo(BackupContract.TYPE_CREDENTIALS, DataKind.CREDENTIALS, 0),
                    ),
                needsRestart = true,
                reEnrolOnRestore = listOf("Fingerprint unlock"),
            )

        assertThat(AgentInfo.fromBundle(parcel(testSubject.toBundle()))).isEqualTo(testSubject)
    }

    @Test
    fun providerInfoSurvivesAParcel() {
        val testSubject = ProviderInfo(BackupContract.VERSION, "Pvot Cloud", setOf("BACKUP"))

        assertThat(ProviderInfo.fromBundle(parcel(testSubject.toBundle()))).isEqualTo(testSubject)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val bundle = DataTypeInfo("photos", DataKind.CUSTOM, 1).toBundle()
        bundle.putString("addedInVersion2", "ignored")

        assertThat(DataTypeInfo.fromBundle(parcel(bundle)))
            .isEqualTo(DataTypeInfo("photos", DataKind.CUSTOM, 1))
    }

    private fun parcel(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        try {
            bundle.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return Bundle.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}
