// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.index

import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.pack.PackedBlob

data class BlobLocation(val packId: Id, val blob: PackedBlob)
