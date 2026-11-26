// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.pack

import org.prauga.cloud.repo.Id

class Pack(val id: Id, val data: ByteArray, val blobs: List<PackedBlob>)
