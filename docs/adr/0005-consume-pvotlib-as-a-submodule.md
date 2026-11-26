<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# ADR: Consume PvotLib as a Submodule Until It Releases the Cloud Components

- Issue: none
- Status: **Accepted**
- Date: 2026-11-27

## Context

The template consumes PvotLib from Maven (`com.prauga.pvot:*`) with an optional `pvotLibDir` for
local development. Pvot Cloud needs components that no PvotLib release has yet:
`PvotStatusDot`, `PvotTileGrid` and `PvotOnboardingSheet` with its `secure` flag, all on PvotLib's
`feat/cloud-components` branch. Changes to them are made while the app is built.

## Decision

Pvot Cloud keeps PvotLib as the `PvotLib/` git submodule, included with `includeBuild("PvotLib")`,
until a PvotLib release contains these components (maintainer, 2026-11-27). Then one commit
switches to the Maven artifacts and `pvotLibDir`, removes the submodule and marks this ADR
**Obsolete**.

## Outcomes

### Positive Outcomes

- New components are built and tried in the app in one composite build, without a release for each
  change.

### Negative Outcomes

- Cloning needs `--recurse-submodules`, and the submodule must point at a commit that is pushed
  to PvotLib.
- The app builds against unreleased PvotLib code, so a PvotLib change can break it until the
  release.
