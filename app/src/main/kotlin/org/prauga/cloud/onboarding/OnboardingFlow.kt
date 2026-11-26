// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import org.prauga.cloud.R

@Composable
fun OnboardingFlow(
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(visible) { if (visible) viewModel.onEvent(OnboardingEvent.Start) }
    val pickFolder =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            viewModel.onEvent(OnboardingEvent.LocationPicked(uri?.toString()))
        }
    OnboardingSheets(
        state = state,
        visible = visible,
        onEvent = viewModel::onEvent,
        onPickFolder = { pickFolder.launch(null) },
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingSheets(
    state: OnboardingUiState,
    visible: Boolean,
    onEvent: (OnboardingEvent) -> Unit,
    onPickFolder: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.step) {
        OnboardingStep.WELCOME -> WelcomeSheet(visible, onEvent, onClose, modifier)
        OnboardingStep.LOCATION -> LocationSheet(state, visible, onPickFolder, onClose, modifier)
        OnboardingStep.PHRASE -> PhraseSheet(state, visible, onEvent, modifier)
        OnboardingStep.CONFIRM -> ConfirmSheet(state, visible, onEvent, modifier)
        OnboardingStep.CATEGORIES -> CategoriesSheet(state, visible, onEvent, modifier)
        OnboardingStep.DONE -> DoneSheet(state, visible, onClose, modifier)
    }
}

@Composable
private fun WelcomeSheet(
    visible: Boolean,
    onEvent: (OnboardingEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotOnboardingSheet(
        visible = visible,
        title = stringResource(R.string.onboarding_welcome_title),
        message = stringResource(R.string.onboarding_welcome_message),
        icon = { StepIcon(R.drawable.ic_t_shield_check) },
        primaryActionLabel = stringResource(R.string.onboarding_continue),
        onPrimaryAction = { onEvent(OnboardingEvent.Continue) },
        secondaryActionLabel = stringResource(R.string.onboarding_not_now),
        onSecondaryAction = onClose,
        onDismissRequest = onClose,
        modifier = modifier,
    )
}

@Composable
private fun LocationSheet(
    state: OnboardingUiState,
    visible: Boolean,
    onPickFolder: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotOnboardingSheet(
        visible = visible,
        title = stringResource(R.string.onboarding_location_title),
        message = stringResource(R.string.onboarding_location_message),
        icon = { StepIcon(R.drawable.ic_t_folder) },
        primaryActionLabel = stringResource(R.string.onboarding_location_choose),
        onPrimaryAction = onPickFolder,
        primaryActionEnabled = !state.busy,
        onDismissRequest = onClose,
        modifier = modifier,
    ) {
        if (state.locationFailed)
            Notice(stringResource(R.string.onboarding_location_failed), error = true)
    }
}

@Composable
private fun DoneSheet(
    state: OnboardingUiState,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotOnboardingSheet(
        visible = visible,
        title = stringResource(R.string.onboarding_done_title),
        message = stringResource(R.string.onboarding_done_message, state.locationName.orEmpty()),
        icon = { StepIcon(R.drawable.ic_t_lock) },
        primaryActionLabel = stringResource(R.string.onboarding_done),
        onPrimaryAction = onClose,
        onDismissRequest = onClose,
        modifier = modifier,
    )
}

@Composable
internal fun StepIcon(@DrawableRes res: Int, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(56.dp),
    )
}

@Composable
internal fun Notice(text: String, error: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (error) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
