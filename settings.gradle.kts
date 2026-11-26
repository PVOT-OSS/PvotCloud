// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://pvot-oss.github.io/PvotLib/maven") {
            content { includeGroup("com.prauga.pvot") }
        }
    }
}

rootProject.name = "PvotCloud"

include(":app")

include(":core:model")

include(":core:data")

include(":core:datastore")

include(":core:keys")

include(":repo")

include(":storage")

include(":sources")

include(":sdk:api")

include(":sdk:agent")

include(":sdk-host")

include(":samples:notes")

include(":samples:ledger")

include(":samples:vault")

include(":samples:optout")

includeBuild("PvotLib")
