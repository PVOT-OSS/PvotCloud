// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotOnboardingSheet
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.cloud.R
import org.prauga.cloud.core.model.BackupCategory

@Composable
fun OnboardingFlow(
    visible: Boolean,
    onClose: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    LaunchedEffect(visible) { if (visible) onEvent(OnboardingEvent.Start) }
    val pickFolder =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            onEvent(OnboardingEvent.LocationPicked(uri?.toString()))
        }
    val icon: @Composable (Int) -> Unit = { res -> StepIcon(res) }

    when (state.step) {
        OnboardingStep.WELCOME ->
            PvotOnboardingSheet(
                visible = visible,
                title = stringResource(R.string.onboarding_welcome_title),
                message = stringResource(R.string.onboarding_welcome_message),
                icon = { icon(R.drawable.ic_t_shield_check) },
                primaryActionLabel = stringResource(R.string.onboarding_continue),
                onPrimaryAction = { onEvent(OnboardingEvent.Continue) },
                secondaryActionLabel = stringResource(R.string.onboarding_not_now),
                onSecondaryAction = onClose,
                onDismissRequest = onClose,
            )
        OnboardingStep.LOCATION ->
            PvotOnboardingSheet(
                visible = visible,
                title = stringResource(R.string.onboarding_location_title),
                message = stringResource(R.string.onboarding_location_message),
                icon = { icon(R.drawable.ic_t_folder) },
                primaryActionLabel = stringResource(R.string.onboarding_location_choose),
                onPrimaryAction = { pickFolder.launch(null) },
                primaryActionEnabled = !state.busy,
                onDismissRequest = onClose,
            ) {
                if (state.locationFailed)
                    Notice(stringResource(R.string.onboarding_location_failed), error = true)
            }
        OnboardingStep.PHRASE ->
            PvotOnboardingSheet(
                visible = visible,
                title = stringResource(R.string.onboarding_phrase_title),
                message = stringResource(R.string.onboarding_phrase_message),
                icon = { icon(R.drawable.ic_t_key) },
                primaryActionLabel = stringResource(R.string.onboarding_phrase_written),
                onPrimaryAction = { onEvent(OnboardingEvent.PhraseWritten) },
                dismissible = false,
                secure = true,
            ) {
                WordGrid(state.words)
            }
        OnboardingStep.CONFIRM ->
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
            ) {
                if (state.answersWrong)
                    Notice(stringResource(R.string.onboarding_confirm_wrong), error = true)
                state.questions.forEach { question ->
                    Question(question, state.answers[question.position]) { word ->
                        onEvent(OnboardingEvent.Answer(question.position, word))
                    }
                }
            }
        OnboardingStep.CATEGORIES ->
            PvotOnboardingSheet(
                visible = visible,
                title = stringResource(R.string.onboarding_categories_title),
                message = stringResource(R.string.onboarding_categories_message),
                primaryActionLabel = stringResource(R.string.onboarding_continue),
                onPrimaryAction = { onEvent(OnboardingEvent.SaveCategories) },
                primaryActionEnabled = state.categories.isNotEmpty() && !state.busy,
                dismissible = false,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    BackupCategory.entries.forEach { category ->
                        val (title, res) = categoryText(category)
                        PvotSwitchRow(
                            title = stringResource(title),
                            checked = category in state.categories,
                            onCheckedChange = {
                                onEvent(OnboardingEvent.SetCategory(category, it))
                            },
                            icon = { Icon(painterResource(res), contentDescription = null) },
                        )
                    }
                }
            }
        OnboardingStep.DONE ->
            PvotOnboardingSheet(
                visible = visible,
                title = stringResource(R.string.onboarding_done_title),
                message =
                    stringResource(R.string.onboarding_done_message, state.locationName.orEmpty()),
                icon = { icon(R.drawable.ic_t_lock) },
                primaryActionLabel = stringResource(R.string.onboarding_done),
                onPrimaryAction = onClose,
                onDismissRequest = onClose,
            )
    }
}

@Composable
private fun StepIcon(@DrawableRes res: Int) {
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp),
    )
}

@Composable
private fun Notice(text: String, error: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (error) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WordGrid(words: List<String>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
        words.chunked(COLUMNS).forEachIndexed { row, chunk ->
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

@Composable
private fun Question(question: ConfirmQuestion, answer: String?, onAnswer: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PvotSpacing.XS)) {
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
