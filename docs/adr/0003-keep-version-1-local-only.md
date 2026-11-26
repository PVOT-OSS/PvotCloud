<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# ADR: Keep Version 1 Local Only

- Issue: none
- Status: **Proposed**
- Date: 2026-11-27

## Context

The maintainer set the scope of version 1 on 2026-11-26: a local backup and restore pipeline and a
base SDK, published on Play. Cloud accounts, sync and credentials come later. A backup app reads
photos, contacts, calendars and other apps' data, so what it can send off the phone is the first
thing a careful user asks about.

## Decision

Version 1 writes only to folders the user picks through the Storage Access Framework: internal
storage, an SD card or a USB drive. The app holds no `INTERNET` permission, so nothing it reads
can leave the device through it. Cloud destinations add the permission in their own release,
together with a privacy policy and data-safety update.

## Outcomes

### Positive Outcomes

- The Play data-safety form is "no data collected or shared", and the manifest proves it.
- No server, account or network failure modes in version 1.

### Negative Outcomes

- A backup kept only on the phone's own storage is lost with the phone. The app has to say so and
  nudge users to copy it to a drive or a computer.
- Network destinations later bring a permission change that users will see on update.
