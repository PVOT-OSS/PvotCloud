// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.pack

import org.prauga.cloud.repo.Id

data class PackedBlob(
    val type: BlobType,
    val id: Id,
    val offset: Long,
    val length: Int,
    val uncompressedLength: Int?,
)
