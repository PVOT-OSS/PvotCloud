// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.tree

enum class NodeType(internal val json: String) {
    FILE("file"),
    DIR("dir"),
    SYMLINK("symlink"),
    DEV("dev"),
    CHARDEV("chardev"),
    FIFO("fifo"),
    SOCKET("socket"),
    IRREGULAR("irregular"),
}
