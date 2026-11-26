<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Privacy Policy — Pvot Cloud

_Effective 27 November 2026_

Pvot Cloud (package `org.prauga.cloud`) is an app published by Saalim Quadri for PVOT-OSS. This
policy explains what the app does with your data.

**In short: the app has no accounts, no ads, no analytics and no tracking. It has no internet
permission. It copies your data only to the folder or drive you choose, encrypted with a recovery
phrase that only you hold.**

## What the app stores

- On your phone: the folder you chose, which kinds of data to back up, when the last backup ran
  and how many items it saved. Your recovery phrase is stored encrypted with a key kept in the
  phone's secure hardware (Android Keystore) and is never written anywhere in plain text.
- In the folder you chose: an encrypted backup in the open restic format. Without your recovery
  phrase nobody, including us, can read it.

## Permissions

- **Photos and videos** (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`,
  or `READ_EXTERNAL_STORAGE` on Android 12 and older): to back up your photos and videos. If you
  allow only some photos, only those are backed up.
- **Photo location** (`ACCESS_MEDIA_LOCATION`): to copy photos with the location they were taken at
  intact, so a restored photo is identical to the original.
- **Notifications** (`POST_NOTIFICATIONS`): to show a backup's progress.
- **Foreground service** (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`): to keep a backup
  running when you leave the app.

The folder you choose is shared with the app through Android's folder picker; the app cannot see
other folders.

## Backups

Your recovery phrase is excluded from Android's own device backup. The app's settings may be
included in your device backup and in a transfer to a new device.

## Changes

Changes to this policy are published with the app's source and take effect with the release that
ships them.

## Contact

Open an issue at <https://github.com/PVOT-OSS/PvotCloud/issues>.
