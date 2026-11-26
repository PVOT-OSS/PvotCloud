<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# ADR: Ship the Backup SDK

- Issue: none
- Status: **Proposed**
- Date: 2026-11-27

## Context

Pvot Cloud backs up what Android lets a normal app read. Other apps' private data, sign-ins most of
all, is out of reach unless the app hands it over. The maintainer asked for an open SDK "with a
provider for app data, creds, etc." so that any app that adds it shows up in Pvot Cloud. The
earlier spec (apps-skills `sdk.md`) planned a separate repository and kept credentials for a later
version.

## Decision

- **Modules in this repository for now:** `:sdk:api` and `:sdk:agent` (Apache-2.0, so any app may
  embed them) and `:sdk-host` (AGPL, the provider side in Pvot Cloud). They move to their own
  repository and Maven Central when the contract is frozen.
- **Contract:** AIDL with fixed transaction ids; models travel as `Bundle`s so fields can be added
  without breaking older apps. Data never crosses Binder; it streams through reliable pipes.
- **Kinds of data:** app data (zero-code default or declared), credentials (a logical JSON export
  the app re-encrypts on import) and custom types, all in the BACKUP capability.
- **Trust:** the agent checks the caller's signing certificate on every call. A provider is accepted
  when the app pins its certificate or the user approves it in a dialog inside the app. The
  provider records the app's certificate and restores only to the same package and certificate.
- **Storage:** each included app's exports go into the snapshot under `apps/<package>/` with an
  `app.json`, so they share the repository's encryption and deduplication.

## Outcomes

### Positive Outcomes

- Any app gets encrypted, restic-readable backups of its data and sign-ins with one dependency.
- The approval happens inside the app, so a hostile provider can't read data without the user
  seeing the app's own dialog.

### Negative Outcomes

- Clearing an app's data also clears its approval, so a restore asks again.
- Until a release certificate is pinned in the library, every app needs the user's approval.
- The repository holds code under two licences.
