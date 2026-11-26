// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.home

import android.os.Build
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotStatus
import com.prauga.pvot.designsystem.components.PvotStatusDot
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
import org.prauga.cloud.core.model.CategoryStatus
import org.prauga.cloud.core.model.RunProgress

@Composable
internal fun StorageCard(overview: BackupOverview, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val location = overview.location
    PvotCard(modifier) {
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
internal fun StatusCard(
    overview: BackupOverview,
    running: RunProgress?,
    modifier: Modifier = Modifier,
) {
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
    PvotListGroup(modifier) {
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

@Composable
internal fun ValueRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    value: String,
    modifier: Modifier = Modifier,
) {
    PvotListRow(
        modifier = modifier,
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
internal fun verifiedText(instant: Instant?): String =
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

internal class CategoryTile(val title: String, val value: String, @DrawableRes val icon: Int)

@Composable
internal fun tileFor(status: CategoryStatus): CategoryTile {
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
