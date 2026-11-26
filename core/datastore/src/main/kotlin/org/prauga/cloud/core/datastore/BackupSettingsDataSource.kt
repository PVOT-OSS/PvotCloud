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
import org.prauga.cloud.core.model.AppRecord
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupOutcome
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.CategoryCount
import org.prauga.cloud.core.model.IncludedApp
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
                includedApps =
                    preferences[INCLUDED_APPS].orEmpty().mapNotNull(::includedApp).toSet(),
                appRecords = appRecords(preferences),
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

    suspend fun include(app: IncludedApp) {
        dataStore.edit { preferences ->
            val others =
                preferences[INCLUDED_APPS].orEmpty().filterNot {
                    it.startsWith(app.packageName + SEPARATOR)
                }
            preferences[INCLUDED_APPS] =
                (others + (app.packageName + SEPARATOR + app.certificate)).toSet()
        }
    }

    suspend fun exclude(packageName: String) {
        dataStore.edit { preferences ->
            preferences[INCLUDED_APPS] =
                preferences[INCLUDED_APPS]
                    .orEmpty()
                    .filterNot { it.startsWith(packageName + SEPARATOR) }
                    .toSet()
        }
    }

    suspend fun recordApp(record: AppRecord) {
        dataStore.edit { preferences ->
            preferences[appAtKey(record.packageName)] = record.finishedAt.toEpochMilli()
            preferences[appOutcomeKey(record.packageName)] = record.outcome.name
            val detail = record.detail
            if (detail == null) preferences.remove(appDetailKey(record.packageName))
            else preferences[appDetailKey(record.packageName)] = detail
        }
    }

    suspend fun recordFailure(finishedAt: Instant) {
        dataStore.edit {
            it[LAST_BACKUP_AT] = finishedAt.toEpochMilli()
            it[LAST_BACKUP_OUTCOME] = BackupOutcome.FAILED.name
        }
    }

    private fun includedApp(stored: String): IncludedApp? {
        val packageName = stored.substringBefore(SEPARATOR, "")
        val certificate = stored.substringAfter(SEPARATOR, "")
        return if (packageName.isEmpty() || certificate.isEmpty()) null
        else IncludedApp(packageName, certificate)
    }

    private fun appRecords(preferences: Preferences): Map<String, AppRecord> =
        preferences
            .asMap()
            .keys
            .map { it.name }
            .filter { it.startsWith(APP_AT) }
            .mapNotNull { name ->
                val packageName = name.removePrefix(APP_AT)
                val at = preferences[appAtKey(packageName)]
                val outcome =
                    BackupOutcome.entries.firstOrNull {
                        it.name == preferences[appOutcomeKey(packageName)]
                    }
                if (at == null || outcome == null) null
                else
                    AppRecord(
                        packageName,
                        Instant.ofEpochMilli(at),
                        outcome,
                        preferences[appDetailKey(packageName)],
                    )
            }
            .associateBy { it.packageName }

    private fun lastBackup(preferences: Preferences): LastBackup? {
        val at = preferences[LAST_BACKUP_AT]
        val outcome =
            BackupOutcome.entries.firstOrNull { it.name == preferences[LAST_BACKUP_OUTCOME] }
        return if (at != null && outcome != null) LastBackup(Instant.ofEpochMilli(at), outcome)
        else null
    }

    private companion object {
        val LOCATION_URI = stringPreferencesKey("location_uri")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val CATEGORIES = stringSetPreferencesKey("categories")
        val PHRASE_CONFIRMED = booleanPreferencesKey("phrase_confirmed")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_OUTCOME = stringPreferencesKey("last_backup_outcome")
        val INCLUDED_APPS = stringSetPreferencesKey("included_apps")
        const val SEPARATOR = "|"
        const val APP_AT = "app_at_"

        fun appAtKey(packageName: String) = longPreferencesKey(APP_AT + packageName)

        fun appOutcomeKey(packageName: String) = stringPreferencesKey("app_outcome_$packageName")

        fun appDetailKey(packageName: String) = stringPreferencesKey("app_detail_$packageName")

        fun itemsKey(category: BackupCategory) =
            intPreferencesKey("items_${category.name.lowercase()}")

        fun bytesKey(category: BackupCategory) =
            longPreferencesKey("bytes_${category.name.lowercase()}")
    }
}
