// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import org.prauga.cloud.core.model.BackupCategory

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val busy: Boolean = false,
    val locationName: String? = null,
    val locationFailed: Boolean = false,
    val words: List<String> = emptyList(),
    val questions: List<ConfirmQuestion> = emptyList(),
    val answers: Map<Int, String> = emptyMap(),
    val answersWrong: Boolean = false,
    val categories: Set<BackupCategory> = BackupCategory.entries.toSet(),
) {
    val allAnswered: Boolean
        get() = questions.isNotEmpty() && questions.all { it.position in answers }
}
