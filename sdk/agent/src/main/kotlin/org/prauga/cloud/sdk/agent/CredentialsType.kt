// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

internal class CredentialsType(
    val export: () -> List<Credential>,
    val import: (List<Credential>) -> Unit,
)
