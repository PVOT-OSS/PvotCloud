// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.check

import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.pack.BlobType

sealed interface CheckProblem {
    data class MissingPack(val packId: Id) : CheckProblem

    data class UnindexedPack(val packId: Id) : CheckProblem

    data class DamagedPack(val packId: Id) : CheckProblem

    data class HeaderMismatch(val packId: Id) : CheckProblem

    data class UnreadableSnapshot(val snapshotId: Id) : CheckProblem

    data class MissingBlob(val snapshotId: Id, val type: BlobType, val blobId: Id) : CheckProblem
}
