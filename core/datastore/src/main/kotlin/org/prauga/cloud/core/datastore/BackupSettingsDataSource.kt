// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupOutcome
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.CategoryCount
import org.prauga.cloud.core.model.LastBackup
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
                lastBackup = lastBackup(preferences),
                counts =
                    BackupCategory.entries
                        .mapNotNull { category ->
                            val items = preferences[itemsKey(category)] ?: return@mapNotNull null
                            category to CategoryCount(items, preferences[bytesKey(category)] ?: 0L)
                        }
                        .toMap(),
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

    suspend fun recordSuccess(finishedAt: Instant, counts: Map<BackupCategory, CategoryCount>) {
        dataStore.edit { preferences ->
            preferences[LAST_BACKUP_AT] = finishedAt.toEpochMilli()
            preferences[LAST_BACKUP_OUTCOME] = BackupOutcome.SUCCESS.name
            BackupCategory.entries.forEach { category ->
                val count = counts[category]
                if (count == null) {
                    preferences.remove(itemsKey(category))
                    preferences.remove(bytesKey(category))
                } else {
                    preferences[itemsKey(category)] = count.items
                    preferences[bytesKey(category)] = count.bytes
                }
            }
        }
    }

    suspend fun recordFailure(finishedAt: Instant) {
        dataStore.edit {
            it[LAST_BACKUP_AT] = finishedAt.toEpochMilli()
            it[LAST_BACKUP_OUTCOME] = BackupOutcome.FAILED.name
        }
    }

    private fun lastBackup(preferences: Preferences): LastBackup? {
        val at = preferences[LAST_BACKUP_AT] ?: return null
        val outcome =
            BackupOutcome.entries.firstOrNull { it.name == preferences[LAST_BACKUP_OUTCOME] }
                ?: return null
        return LastBackup(Instant.ofEpochMilli(at), outcome)
    }

    private companion object {
        val LOCATION_URI = stringPreferencesKey("location_uri")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val CATEGORIES = stringSetPreferencesKey("categories")
        val PHRASE_CONFIRMED = booleanPreferencesKey("phrase_confirmed")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_OUTCOME = stringPreferencesKey("last_backup_outcome")

        fun itemsKey(category: BackupCategory) =
            intPreferencesKey("items_${category.name.lowercase()}")

        fun bytesKey(category: BackupCategory) =
            longPreferencesKey("bytes_${category.name.lowercase()}")
    }
}
