// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.home

import android.Manifest
import android.os.Build
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotStatus
import com.prauga.pvot.designsystem.components.PvotStatusDot
import com.prauga.pvot.designsystem.components.PvotTileGrid
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.components.progress.PvotLinearProgress
import com.prauga.pvot.designsystem.components.progress.PvotProportionBar
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.time.Instant
import org.prauga.cloud.R
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupOutcome
import org.prauga.cloud.core.model.BackupOverview
import org.prauga.cloud.core.model.BackupSchedule
import org.prauga.cloud.core.model.CategoryStatus
import org.prauga.cloud.core.model.RunProgress
import org.prauga.cloud.onboarding.OnboardingFlow

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, viewModel::onEvent)
}

@Composable
private fun HomeContent(state: UiState<HomeData>, onEvent: (HomeEvent) -> Unit) {
    when (state) {
        UiState.Loading -> PvotLoadingContent()
        is UiState.Error ->
            PvotErrorContent(
                title = stringResource(R.string.home_error_title),
                message = stringResource(R.string.home_error_message),
                onRetry = { onEvent(HomeEvent.Retry) },
            )
        is UiState.Success -> Overview(state.data.overview, state.data.running, onEvent)
    }
}

@Composable
private fun Overview(
    overview: BackupOverview,
    running: RunProgress?,
    onEvent: (HomeEvent) -> Unit,
) {
    var settingUp by rememberSaveable { mutableStateOf(false) }
    OnboardingFlow(visible = settingUp, onClose = { settingUp = false })
    val onSetUp = { settingUp = true }
    val askThenBackUp =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onEvent(HomeEvent.BackUpNow)
        }
    val setUp = overview.location != null && overview.recoveryPhraseSaved
    PvotScreen(Modifier.fillMaxSize()) {
        item { Header() }
        item { StorageCard(overview) }
        item { StatusCard(overview, running) }
        if (!setUp) {
            item {
                PvotButton(
                    text = stringResource(R.string.home_set_up),
                    onClick = onSetUp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (running == null) {
            item {
                PvotButton(
                    text = stringResource(R.string.home_back_up_now),
                    onClick = { askThenBackUp.launch(backupPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            val tiles = overview.categories.map { status -> tileFor(status) }
            PvotTileGrid(
                title =
                    stringResource(
                        if (overview.lastBackup == null) R.string.home_contents_planned
                        else R.string.home_contents_saved
                    ),
                status = if (overview.lastBackup == null) PvotStatus.Neutral else PvotStatus.Good,
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
        item {
            PvotListGroup(Modifier.fillMaxWidth()) {
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
        item {
            PvotListGroup(Modifier.fillMaxWidth()) {
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
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth().padding(horizontal = PvotSpacing.S, vertical = PvotSpacing.M)) {
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

@Composable
private fun StorageCard(overview: BackupOverview) {
    val context = LocalContext.current
    val location = overview.location
    PvotCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(PvotSpacing.L),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_storage),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                val used = location?.usedBytes
                val total = location?.totalBytes
                Text(
                    text =
                        when {
                            location == null -> stringResource(R.string.home_storage_none)
                            used != null && total != null ->
                                stringResource(
                                    R.string.home_storage_used,
                                    Formatter.formatShortFileSize(context, used),
                                    Formatter.formatShortFileSize(context, total),
                                )
                            else -> location.name
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val used = location?.usedBytes
            val total = location?.totalBytes
            val share =
                if (used == null || total == null || total <= 0) 0f
                else (used.toFloat() / total).coerceIn(0f, 1f)
            PvotProportionBar(segments = listOf(share), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatusCard(overview: BackupOverview, running: RunProgress?) {
    val context = LocalContext.current
    val last = overview.lastBackup
    val (status, title) =
        when {
            running != null -> PvotStatus.Warning to R.string.home_status_running
            last == null -> PvotStatus.Neutral to R.string.home_status_never
            last.outcome == BackupOutcome.SUCCESS -> PvotStatus.Good to R.string.home_status_done
            else -> PvotStatus.Error to R.string.home_status_failed
        }
    val supporting =
        when {
            running == null -> stringResource(R.string.home_status_device, Build.MODEL)
            running.totalFiles == 0 -> stringResource(R.string.home_status_preparing)
            else ->
                pluralStringResource(
                    R.plurals.home_status_running_detail,
                    running.totalFiles,
                    running.files,
                    running.totalFiles,
                    Formatter.formatShortFileSize(context, running.bytes),
                    Formatter.formatShortFileSize(context, running.totalBytes),
                )
        }
    PvotListGroup(Modifier.fillMaxWidth()) {
        item {
            Column {
                PvotListRow(
                    title = stringResource(title),
                    supportingText = supporting,
                    leading = { PvotStatusDot(status, Modifier.padding(PvotSpacing.S)) },
                    trailing =
                        if (running == null)
                            last?.let { { TrailingText(relativeTime(it.finishedAt)) } }
                        else null,
                )
                if (running != null) {
                    val share =
                        if (running.totalBytes <= 0) 0f
                        else (running.bytes.toFloat() / running.totalBytes).coerceIn(0f, 1f)
                    PvotLinearProgress(
                        progress = { share },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.S),
                    )
                }
            }
        }
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

@Composable
private fun ValueRow(@DrawableRes icon: Int, @StringRes title: Int, value: String) {
    PvotListRow(
        title = stringResource(title),
        leading = {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        },
        trailing = { TrailingText(value) },
    )
}

@Composable
private fun TrailingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun verifiedText(instant: Instant?): String =
    if (instant == null) stringResource(R.string.home_value_never)
    else stringResource(R.string.home_value_checked, relativeTime(instant))

@Composable
private fun relativeTime(instant: Instant): String =
    if (System.currentTimeMillis() - instant.toEpochMilli() < DateUtils.MINUTE_IN_MILLIS) {
        stringResource(R.string.home_just_now)
    } else {
        relativeSpan(instant)
    }

private fun relativeSpan(instant: Instant): String =
    DateUtils.getRelativeTimeSpanString(
            instant.toEpochMilli(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        )
        .toString()

private class CategoryTile(val title: String, val value: String, @DrawableRes val icon: Int)

@Composable
private fun tileFor(status: CategoryStatus): CategoryTile {
    val (title, icon) =
        when (status.category) {
            BackupCategory.PHOTOS -> R.string.category_photos to R.drawable.ic_t_photo
            BackupCategory.CONTACTS -> R.string.category_contacts to R.drawable.ic_t_user
            BackupCategory.CALENDARS -> R.string.category_calendars to R.drawable.ic_t_calendar
            BackupCategory.MESSAGES -> R.string.category_messages to R.drawable.ic_t_message_circle
            BackupCategory.APPS -> R.string.category_apps to R.drawable.ic_t_puzzle
            BackupCategory.FOLDERS -> R.string.category_folders to R.drawable.ic_t_folder
        }
    val bytes = status.bytes
    val items = status.items
    val value =
        when {
            items != null -> pluralStringResource(R.plurals.home_items, items, items)
            bytes != null -> Formatter.formatShortFileSize(LocalContext.current, bytes)
            else -> stringResource(R.string.home_value_not_yet)
        }
    return CategoryTile(stringResource(title), value, icon)
}
