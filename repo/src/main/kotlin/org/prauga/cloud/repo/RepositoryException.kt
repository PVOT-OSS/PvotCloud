// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

sealed class RepositoryException(message: String) : Exception(message) {

    class WrongPassword : RepositoryException("no key in the repository matches the password")

    class AlreadyExists : RepositoryException("a repository already exists at this location")

    class Unsupported(version: Int) :
        RepositoryException("unsupported repository version $version")
}
