<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Pvot Cloud SDK

Let users back up and restore your app's data, sign-ins included, with Pvot Cloud or any other
provider that implements the same open contract. Backups are end-to-end encrypted by the provider;
your app needs no account, server or network permission. Apache-2.0.

| module | what |
|---|---|
| `:sdk:api` | the AIDL contract (`IBackupAgent`, `IBackupProvider`, `IResultCallback`) and its models |
| `:sdk:agent` | the library your app adds: the agent service, the approval dialog, app-data and credentials export |

## Zero code

Add the dependency. If your app allows Android backups (`android:allowBackup="true"`, the default),
your files, databases, shared preferences, other app folders and external app files are backed up.
`cache`, `code_cache` and `no_backup` are left out. Databases are copied consistently.

```kotlin
dependencies { implementation(project(":sdk:agent")) }
```

The library adds an exported agent service and an approval activity to your manifest. Pvot Cloud
finds your app through the service.

## Choosing what to back up

Name a configuration class in the manifest:

```xml
<meta-data
    android:name="org.prauga.cloud.sdk.CONFIG"
    android:value="com.example.ledger.LedgerBackup" />
```

```kotlin
class LedgerBackup : BackupAgentConfigProvider {
    override fun backupAgentConfig(context: Context) = backupAgent {
        schemaVersion = 2
        database("ledger.db")
        sharedPreferences("settings")
        dataStore("prefs")
        files("presets")
        custom(
            "budget",
            export = { out -> out.write(readBudget()) },
            import = { source, fromSchema -> writeBudget(source, fromSchema) },
        )
        credentials(export = { vault.all() }, import = { vault.replaceAll(it) })
        reEnrolOnRestore("Turn fingerprint unlock on again")
    }
}
```

- `everything()` keeps the zero-code set and lets you add custom types.
- `import` must accept every older `schemaVersion` you shipped. A backup from a newer schema is
  refused with `SCHEMA_TOO_NEW`.
- An app with `allowBackup="false"` and no configuration is listed but skipped.
- The configuration class needs a public no-argument constructor; the library's R8 rules keep it.

## Sign-ins and the Keystore

Data encrypted with an Android Keystore key can't be restored as bytes: the key never leaves the
phone. Export such data in readable form through `credentials` or `custom` and encrypt it again
with a new key on import. Pvot Cloud stores it inside its own encrypted backup. Anything that truly
can't move goes in `reEnrolOnRestore`, which Pvot Cloud shows after a restore.

## Who may read your data

Every call checks the caller's signing certificate. A provider is accepted when:

- the user allowed it in the dialog the library shows inside your app (Pvot Cloud opens it when the
  user turns your app on, and again after your app's data is cleared), or
- you list its certificate SHA-256 in the manifest:
  `<meta-data android:name="org.prauga.cloud.sdk.TRUSTED_PROVIDERS" android:value="ab12…,cd34…" />`.

`TrustedProviders.revokeAll(context)` withdraws the user's approvals.

## Restore

The provider restores app data first, then sign-ins, then custom types, and calls
`restoreFinished()`. Your `onRestoreFinished` block runs, and the process restarts when
`needsRestart` is true (the default) or when shared preferences were replaced, because Android
caches them in memory.

## Formats

- `app-data`: a POSIX tar archive with `file/`, `db/`, `sp/`, `root/` and `ext/` folders.
- `credentials`: JSON, `{"format":"org.prauga.cloud.sdk.credentials","version":1,"credentials":[…]}`.
- custom types: whatever your `export` writes.

With restic and the recovery phrase, a backup can be opened on a computer: each app is under
`apps/<package>/`, next to an `app.json` describing it.

## Samples

`samples/notes` (zero code), `samples/ledger` (declared data, a custom type and schema 2),
`samples/vault` (Keystore-encrypted sign-ins) and `samples/optout` (`allowBackup="false"`).
