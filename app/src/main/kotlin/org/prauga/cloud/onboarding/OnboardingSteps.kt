// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotOnboardingSheet
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.cloud.R
import org.prauga.cloud.core.model.BackupCategory

@Composable
internal fun PhraseSheet(
    state: OnboardingUiState,
    visible: Boolean,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotOnboardingSheet(
        visible = visible,
        title = stringResource(R.string.onboarding_phrase_title),
        message = stringResource(R.string.onboarding_phrase_message),
        icon = { StepIcon(R.drawable.ic_t_key) },
        primaryActionLabel = stringResource(R.string.onboarding_phrase_written),
        onPrimaryAction = { onEvent(OnboardingEvent.PhraseWritten) },
        dismissible = false,
        secure = true,
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
            state.words.chunked(COLUMNS).forEachIndexed { row, chunk ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
                ) {
                    chunk.forEachIndexed { column, word ->
                        Text(
                            text =
                                stringResource(
                                    R.string.onboarding_word,
                                    row * COLUMNS + column + 1,
                                    word,
                                ),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConfirmSheet(
    state: OnboardingUiState,
    visible: Boolean,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotOnboardingSheet(
        visible = visible,
        title = stringResource(R.string.onboarding_confirm_title),
        message = stringResource(R.string.onboarding_confirm_message),
        primaryActionLabel = stringResource(R.string.onboarding_confirm_check),
        onPrimaryAction = { onEvent(OnboardingEvent.CheckAnswers) },
        primaryActionEnabled = state.allAnswered && !state.busy,
        secondaryActionLabel = stringResource(R.string.onboarding_confirm_show_again),
        onSecondaryAction = { onEvent(OnboardingEvent.ShowPhraseAgain) },
        dismissible = false,
        secure = true,
        modifier = modifier,
    ) {
        if (state.answersWrong)
            Notice(stringResource(R.string.onboarding_confirm_wrong), error = true)
        state.questions.forEach { question ->
            Question(
                question = question,
                answer = state.answers[question.position],
                onAnswer = { word -> onEvent(OnboardingEvent.Answer(question.position, word)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun CategoriesSheet(
    state: OnboardingUiState,
    visible: Boolean,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotOnboardingSheet(
        visible = visible,
        title = stringResource(R.string.onboarding_categories_title),
        message = stringResource(R.string.onboarding_categories_message),
        primaryActionLabel = stringResource(R.string.onboarding_continue),
        onPrimaryAction = { onEvent(OnboardingEvent.SaveCategories) },
        primaryActionEnabled = state.categories.isNotEmpty() && !state.busy,
        dismissible = false,
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth()) {
            BackupCategory.entries.forEach { category ->
                val (title, res) = categoryText(category)
                PvotSwitchRow(
                    title = stringResource(title),
                    checked = category in state.categories,
                    onCheckedChange = { onEvent(OnboardingEvent.SetCategory(category, it)) },
                    icon = { Icon(painterResource(res), contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun Question(
    question: ConfirmQuestion,
    answer: String?,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PvotSpacing.XS)) {
        Text(
            text = stringResource(R.string.onboarding_confirm_word, question.position + 1),
            style = MaterialTheme.typography.titleMedium,
        )
        question.options.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            ) {
                pair.forEach { word ->
                    PvotButton(
                        text = word,
                        onClick = { onAnswer(word) },
                        style =
                            if (word == answer) PvotButtonStyle.Filled else PvotButtonStyle.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun categoryText(category: BackupCategory): Pair<Int, Int> =
    when (category) {
        BackupCategory.PHOTOS -> R.string.category_photos to R.drawable.ic_t_photo
        BackupCategory.CONTACTS -> R.string.category_contacts to R.drawable.ic_t_user
        BackupCategory.CALENDARS -> R.string.category_calendars to R.drawable.ic_t_calendar
        BackupCategory.MESSAGES -> R.string.category_messages to R.drawable.ic_t_message_circle
        BackupCategory.APPS -> R.string.category_apps to R.drawable.ic_t_puzzle
        BackupCategory.FOLDERS -> R.string.category_folders to R.drawable.ic_t_folder
    }

private const val COLUMNS = 3
