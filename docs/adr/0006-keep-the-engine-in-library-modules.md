<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# ADR: Keep the Engine, Storage and Sources in Library Modules

- Issue: none
- Status: **Proposed**
- Date: 2026-11-27

## Context

The template's module rules allow a library module when it holds pure logic worth fast JVM tests,
isolates a heavy dependency, or is used by more than one module. The backup engine is most of the
app's logic, it must be verified byte for byte against restic, and parts of it will be reused by
the SDK and by later cloud destinations.

## Decision

- `:repo` is a pure JVM library: crypto, chunker, packs, index, trees, snapshots, backup, restore
  and check. It depends on no Android or project module, and talks to storage through a `Backend`
  interface and to data through `SourceDirectory`.
- `:storage` implements `Backend` on the Storage Access Framework.
- `:sources` turns MediaStore and, later, contacts, calendars, folders and SDK apps into source
  trees.
- `:app` and `:core:data` connect them; nothing in `:repo` knows about Android.

## Outcomes

### Positive Outcomes

- The engine's tests run on the JVM in seconds and can run against the real `restic` binary.
- A new destination is a new `Backend`; a new kind of data is a new source.

### Negative Outcomes

- Three more modules to build and keep within the dependency rules.
- Android-only behaviour (SAF, MediaStore) is covered only by instrumented tests.
