<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# ADR: Use a Recovery Phrase as the Repository Password

- Issue: none
- Status: **Proposed**
- Date: 2026-11-27

## Context

Every repository is encrypted, local ones included, because USB drives and SD cards get lost. The
password must survive losing the phone, must not be guessable offline, and must not need typing
before every scheduled backup. Phone PINs are short enough to brute-force once a repository is
copied. Seedvault uses a BIP39 phrase for the same reasons.

## Decision

Onboarding generates a 12-word BIP39 phrase (kotlin-bip39) and uses it as the restic password. The
user writes it down and proves it by picking three of its words; the phrase screens use
`FLAG_SECURE`. The phrase is then stored on the device under an AES-256-GCM key in the Android
Keystore (`:core:keys`), in no-backup storage, so scheduled runs can open the repository without a
prompt. There is no escrow to a screen lock or an account.

## Outcomes

### Positive Outcomes

- Restoring on a new phone or a computer needs only the drive and the phrase.
- 128 bits of entropy: a stolen drive can't be brute-forced.
- Scheduled backups run without user interaction.

### Negative Outcomes

- A lost phrase means a lost backup; nobody can recover it.
- Anyone with an unlocked phone and the app can use the stored phrase. Showing it again will be
  gated behind the device credential.
