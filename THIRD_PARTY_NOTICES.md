<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Third-party notices

## Tabler Icons

The app's icons (`app/src/main/res/drawable/ic_t_*.xml`) are converted from
[Tabler Icons](https://tabler.io/icons), Copyright (c) 2020-2026 Paweł Kuna, MIT License. The
license text is in `LICENSES/MIT.txt`.

## restic

`repo/src/main/kotlin/org/prauga/cloud/repo/chunker/` ports the chunker and its polynomial
arithmetic from [restic/chunker](https://github.com/restic/chunker), Copyright (c) 2014 Alexander
Neumann, BSD 2-Clause License, and the repository format follows restic's design document. The
chunk test vectors in `repo/src/test/resources/restic-chunks-*.csv` come from the same project. The
license text is in `LICENSES/BSD-2-Clause.txt`.

## Go

The test helper `repo/src/test/kotlin/org/prauga/cloud/repo/chunker/GoRandom.kt` and
`repo/src/test/resources/go-rng-cooked.txt` reproduce Go's `math/rand` source, Copyright (c) 2009 The
Go Authors, BSD 3-Clause License. The license text is in `LICENSES/BSD-3-Clause.txt`.
