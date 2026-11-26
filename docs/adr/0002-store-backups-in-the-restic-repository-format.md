<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# ADR: Store Backups in the Restic Repository Format

- Issue: none
- Status: **Proposed**
- Date: 2026-11-27

## Context

A backup is only worth something if it can be restored, including when Pvot Cloud itself is gone:
the phone is lost, the app is unpublished, or the user moves to a computer. Backup apps on Play use
private formats (Swift Backup, Google backup) or plain exports with no encryption, deduplication or
integrity check (SMS Backup & Restore). Restoid and Seedvault show that a phone can write a
well-designed open format.

Options considered: a private format (full control, nothing else can read it), plain files or ZIP
archives (readable, but no deduplication or real encryption), Borg (no stable spec outside its
Python code) and restic's repository format v2 (published design document, two independent
implementations in restic and rustic, encryption, deduplication, compression and a check command).

## Decision

Pvot Cloud writes restic repository format v2: scrypt key files, AES-256-CTR with Poly1305-AES,
content-defined chunking, packs with an encrypted header, index files, tree and snapshot JSON, and
zstd compression. The engine is the pure JVM `:repo` module, written from the design document. The
chunker and Go-compatible encodings are ports of restic code under BSD-2-Clause and are listed in
`THIRD_PARTY_NOTICES.md`.

Repositories must stay readable by the `restic` command: `check --read-data` and `restore` work on
what Pvot Cloud writes, and Pvot Cloud reads what restic writes. Interop tests run against a pinned
`restic` binary when `RESTIC_BIN` is set.

## Outcomes

### Positive Outcomes

- A backup can be checked and restored on a computer with `restic` or `rustic` and the recovery
  phrase.
- Deduplication, compression and authenticated encryption come with the format.
- Several devices can share one repository; snapshots are told apart by host.

### Negative Outcomes

- The format is restic's to change. Version 2 is stable, but a version 3 would need a migration.
- Per-file SAF calls are slow, so pack size and listing batching matter more than on a desktop.
- Interop needs a `restic` binary in CI, which the default build does not have.
