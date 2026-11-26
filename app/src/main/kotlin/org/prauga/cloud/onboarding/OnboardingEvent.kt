// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import org.prauga.cloud.core.model.BackupCategory

sealed interface OnboardingEvent {
    data object Start : OnboardingEvent

    data object Continue : OnboardingEvent

    data class LocationPicked(val treeUri: String?) : OnboardingEvent

    data object PhraseWritten : OnboardingEvent

    data class Answer(val position: Int, val word: String) : OnboardingEvent

    data object CheckAnswers : OnboardingEvent

    data object ShowPhraseAgain : OnboardingEvent

    data class SetCategory(val category: BackupCategory, val enabled: Boolean) : OnboardingEvent

    data object SaveCategories : OnboardingEvent
}
