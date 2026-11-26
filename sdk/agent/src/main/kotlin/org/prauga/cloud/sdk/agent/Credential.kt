// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

data class Credential(
    val id: String,
    val label: String,
    val username: String,
    val secret: String,
    val uri: String? = null,
    val notes: String? = null,
) {
    override fun toString(): String = "Credential(id=$id, label=$label)"
}
