// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotStatus
import com.prauga.pvot.designsystem.components.PvotTileGrid
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.cloud.R
import org.prauga.cloud.core.model.BackupOverview
import org.prauga.cloud.core.model.BackupSchedule
import org.prauga.cloud.core.model.RunProgress
import org.prauga.cloud.onboarding.OnboardingFlow

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var settingUp by rememberSaveable { mutableStateOf(false) }
    val askThenBackUp =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.onEvent(HomeEvent.BackUpNow)
        }
    HomeContent(
        state = state,
        onRetry = { viewModel.onEvent(HomeEvent.Retry) },
        onSetUp = { settingUp = true },
        onBackUpNow = { askThenBackUp.launch(backupPermissions()) },
        modifier = modifier,
    )
    OnboardingFlow(visible = settingUp, onClose = { settingUp = false })
}

@Composable
private fun HomeContent(
    state: UiState<HomeData>,
    onRetry: () -> Unit,
    onSetUp: () -> Unit,
    onBackUpNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading -> PvotLoadingContent(modifier)
        is UiState.Error ->
            PvotErrorContent(
                title = stringResource(R.string.home_error_title),
                message = stringResource(R.string.home_error_message),
                onRetry = onRetry,
                modifier = modifier,
            )
        is UiState.Success ->
            Overview(
                overview = state.data.overview,
                running = state.data.running,
                onSetUp = onSetUp,
                onBackUpNow = onBackUpNow,
                modifier = modifier,
            )
    }
}

@Composable
private fun Overview(
    overview: BackupOverview,
    running: RunProgress?,
    onSetUp: () -> Unit,
    onBackUpNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val setUp = overview.location != null && overview.recoveryPhraseSaved
    PvotScreen(modifier.fillMaxSize()) {
        item { Header(Modifier.fillMaxWidth()) }
        item { StorageCard(overview, Modifier.fillMaxWidth()) }
        item { StatusCard(overview, running, Modifier.fillMaxWidth()) }
        if (!setUp) {
            item { WideButton(stringResource(R.string.home_set_up), onSetUp) }
        } else if (running == null) {
            item { WideButton(stringResource(R.string.home_back_up_now), onBackUpNow) }
        }
        item { ContentsGrid(overview) }
        item { SecurityGroup(overview, Modifier.fillMaxWidth()) }
        item { SettingsGroup(overview, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun WideButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PvotButton(text = text, onClick = onClick, modifier = modifier.fillMaxWidth())
}

@Composable
private fun ContentsGrid(overview: BackupOverview, modifier: Modifier = Modifier) {
    val tiles = overview.categories.map { status -> tileFor(status) }
    PvotTileGrid(
        title =
            stringResource(
                if (overview.lastBackup == null) R.string.home_contents_planned
                else R.string.home_contents_saved
            ),
        status = if (overview.lastBackup == null) PvotStatus.Neutral else PvotStatus.Good,
        modifier = modifier,
    ) {
        tiles.forEach { tile ->
            tile(
                title = tile.title,
                value = tile.value,
                icon = { Icon(painterResource(tile.icon), contentDescription = null) },
            )
        }
    }
}

@Composable
private fun SecurityGroup(overview: BackupOverview, modifier: Modifier = Modifier) {
    PvotListGroup(modifier) {
        item {
            ValueRow(
                R.drawable.ic_t_key,
                R.string.home_recovery_phrase,
                stringResource(
                    if (overview.recoveryPhraseSaved) R.string.home_value_saved
                    else R.string.home_value_not_set
                ),
            )
        }
        item {
            ValueRow(
                R.drawable.ic_t_shield_check,
                R.string.home_verify,
                verifiedText(overview.lastVerified),
            )
        }
        item {
            ValueRow(
                R.drawable.ic_t_lock,
                R.string.home_encryption,
                stringResource(R.string.home_value_always_on),
            )
        }
    }
}

@Composable
private fun SettingsGroup(overview: BackupOverview, modifier: Modifier = Modifier) {
    PvotListGroup(modifier) {
        item {
            ValueRow(
                R.drawable.ic_t_folder,
                R.string.home_location,
                overview.location?.name ?: stringResource(R.string.home_value_not_chosen),
            )
        }
        item {
            ValueRow(
                R.drawable.ic_t_clock,
                R.string.home_schedule,
                stringResource(
                    when (overview.schedule) {
                        BackupSchedule.DAILY_WHILE_CHARGING -> R.string.home_schedule_daily
                        BackupSchedule.MANUAL -> R.string.home_schedule_manual
                    }
                ),
            )
        }
        item {
            ValueRow(
                R.drawable.ic_t_puzzle,
                R.string.home_connected_apps,
                if (overview.connectedApps == 0) stringResource(R.string.home_value_none)
                else
                    pluralStringResource(
                        R.plurals.home_apps,
                        overview.connectedApps,
                        overview.connectedApps,
                    ),
            )
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = PvotSpacing.S, vertical = PvotSpacing.M)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = Build.MODEL,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun backupPermissions(): Array<String> =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
