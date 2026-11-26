// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.SavedLocation

class BackupSettingsDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<BackupSettings> =
        dataStore.data.map { preferences ->
            val uri = preferences[LOCATION_URI]
            val name = preferences[LOCATION_NAME]
            BackupSettings(
                location = if (uri != null && name != null) SavedLocation(uri, name) else null,
                categories =
                    preferences[CATEGORIES]
                        ?.mapNotNull { stored ->
                            BackupCategory.entries.firstOrNull { it.name == stored }
                        }
                        ?.toSet() ?: BackupCategory.entries.toSet(),
                phraseConfirmed = preferences[PHRASE_CONFIRMED] ?: false,
            )
        }

    suspend fun setLocation(location: SavedLocation) {
        dataStore.edit {
            it[LOCATION_URI] = location.uri
            it[LOCATION_NAME] = location.name
        }
    }

    suspend fun setCategories(categories: Set<BackupCategory>) {
        dataStore.edit {
            it[CATEGORIES] = categories.mapTo(mutableSetOf()) { category -> category.name }
        }
    }

    suspend fun setPhraseConfirmed(confirmed: Boolean) {
        dataStore.edit { it[PHRASE_CONFIRMED] = confirmed }
    }

    private companion object {
        val LOCATION_URI = stringPreferencesKey("location_uri")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val CATEGORIES = stringSetPreferencesKey("categories")
        val PHRASE_CONFIRMED = booleanPreferencesKey("phrase_confirmed")
    }
}
