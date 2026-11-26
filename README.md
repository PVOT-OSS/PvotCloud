<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Pvot Cloud

Encrypted, verifiable backups of an Android phone to storage you choose: a folder on the phone, an
SD card or a USB drive. Backups use the [restic](https://restic.net) repository format, so they can
be checked and restored on a computer with `restic` and your recovery phrase.

Work in progress. Today the app sets up a backup location and a 12-word recovery phrase, and backs
up photos and videos on demand. Restore, contacts, calendars, scheduling and the SDK for other apps
are next.

- **Private:** nothing leaves the phone. Version 1 has no network permission (see
  [ADR 0003](docs/adr/0003-keep-version-1-local-only.md)).
- **Encrypted:** always, with a key derived from the recovery phrase
  ([ADR 0004](docs/adr/0004-use-a-recovery-phrase-as-the-repository-password.md)).
- **Open:** restic repository format v2
  ([ADR 0002](docs/adr/0002-store-backups-in-the-restic-repository-format.md)).

## Build

PvotLib is a git submodule ([ADR 0005](docs/adr/0005-consume-pvotlib-as-a-submodule.md)):

```sh
git clone --recurse-submodules <repository>
./gradlew assembleDebug
```

JDK 21 and the Android SDK are needed.

## Check

```sh
./gradlew spotlessCheck detekt lintDebug testDebugUnitTest assembleDebug assembleRelease
reuse lint
```

`./gradlew spotlessApply` fixes formatting. The restic interop tests in `:repo` run when
`RESTIC_BIN` points at a `restic` binary and are skipped otherwise. Device tests for the engine and
the SAF backend run with `./gradlew connectedDebugAndroidTest`.

## Layout

| module | what |
|---|---|
| `:app` | screens, the backup worker, DI root |
| `:core:model`, `:core:data`, `:core:datastore`, `:core:keys` | models, repositories, settings, the recovery phrase |
| `:repo` | the backup engine (pure JVM) |
| `:storage` | the engine's backend on the Storage Access Framework |
| `:sources` | what gets backed up (MediaStore today) |

Decisions behind the structure are in [docs/adr](docs/adr).

## Licence

AGPL-3.0-or-later. Parts ported from restic are BSD-2-Clause; see
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
