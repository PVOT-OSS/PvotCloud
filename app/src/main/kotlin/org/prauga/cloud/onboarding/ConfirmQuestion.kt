// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import kotlin.random.Random

data class ConfirmQuestion(val position: Int, val options: List<String>) {

    companion object {
        private const val QUESTIONS = 3
        private const val OPTIONS = 4

        fun create(words: List<String>, random: Random): List<ConfirmQuestion> =
            words.indices.shuffled(random).take(QUESTIONS).sorted().map { position ->
                val wrong =
                    words.filterIndexed { index, word ->
                        index != position && word != words[position]
                    }
                val options =
                    (wrong.distinct().shuffled(random).take(OPTIONS - 1) + words[position])
                        .shuffled(random)
                ConfirmQuestion(position, options)
            }
    }
}
