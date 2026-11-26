// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import java.io.InputStream
import java.io.OutputStream

internal class CustomType(
    val export: (OutputStream) -> Unit,
    val import: (source: InputStream, fromSchema: Int) -> Unit,
)
