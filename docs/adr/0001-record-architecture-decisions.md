<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# ADR: Record Architecture Decisions

- Issue: none
- Status: **Accepted**
- Date: 2026-11-27

## Context

Decisions about modules, layers, libraries and the build shape the app for years, but the reasons
behind them get lost in pull requests and chats. Contributors, human or not, then undo them or
follow them without knowing when they stop applying.

## Decision

Architecture decisions are recorded as ADRs in `docs/adr/`, one Markdown file per decision named
`NNNN-title-with-dashes.md` with the next free four-digit number, written from
[`0000-template.md`](0000-template.md) in Michael Nygard's
[format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

An ADR starts as **Proposed** and becomes **Accepted** or **Rejected** when the maintainers decide.
An accepted ADR isn't rewritten: a later decision supersedes it in a new ADR, and the old one is
marked **Obsolete** with a link to its replacement.

Decisions shared by every PVOT-OSS app (the convention plugins, the quality checks, the module
layout) are recorded in the app template; an app records only its own.

## Outcomes

### Positive Outcomes

- The reason for a structure is next to the code it shapes.
- Changing a decision means writing down what changed and why.

### Negative Outcomes

- Each significant decision costs a short document and a review.
