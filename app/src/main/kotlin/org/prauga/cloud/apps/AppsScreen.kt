// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.apps

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import org.prauga.cloud.R
import org.prauga.cloud.core.model.AppRestoreOutcome
import org.prauga.cloud.core.model.BackupOutcome
import org.prauga.cloud.core.model.ConnectedApp
import org.prauga.cloud.sdk.BackupContract

@Composable
fun AppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<ApprovalRequest?>(null) }
    val approve =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            val request = pending
            pending = null
            if (request != null && result.resultCode == Activity.RESULT_OK) {
                viewModel.onEvent(AppsEvent.Approved(request))
            }
        }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AppsEffect.AskApproval -> {
                    pending = effect.request
                    try {
                        approve.launch(
                            Intent(BackupContract.ACTION_TRUST_PROVIDER)
                                .setPackage(effect.request.packageName)
                        )
                    } catch (_: ActivityNotFoundException) {
                        pending = null
                    }
                }
            }
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(AppsEvent.Refresh)
        onPauseOrDispose {}
    }
    BackHandler(onBack = onBack)
    AppsContent(state, viewModel::onEvent, onBack, modifier)
}

@Composable
private fun AppsContent(
    state: AppsUiState,
    onEvent: (AppsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        PvotTopBar(title = stringResource(R.string.apps_title), onBack = onBack)
        when {
            state.loading -> PvotLoadingContent()
            state.apps.isEmpty() -> PvotEmptyContent(stringResource(R.string.apps_empty))
            else ->
                PvotScreen(Modifier.fillMaxSize()) {
                    state.apps.forEach { app ->
                        item(key = app.packageName) {
                            AppGroup(app, state.restoring, onEvent, Modifier.fillMaxWidth())
                        }
                    }
                }
        }
        RestoreDialog(state.restored, onDismiss = { onEvent(AppsEvent.DismissMessage) })
    }
}

@Composable
private fun AppGroup(
    app: ConnectedApp,
    restoring: String?,
    onEvent: (AppsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotListGroup(modifier) {
        item {
            PvotSwitchRow(
                title = app.label,
                summary = status(app),
                checked = app.included,
                onCheckedChange = { onEvent(AppsEvent.SetIncluded(app.packageName, it)) },
            )
        }
        if (app.lastBackup?.outcome == BackupOutcome.SUCCESS) {
            item {
                PvotListRow(
                    title =
                        stringResource(
                            if (restoring == app.packageName) R.string.apps_restoring
                            else R.string.apps_restore
                        ),
                    leading = {
                        Icon(
                            painterResource(R.drawable.ic_t_restore),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    enabled = restoring == null,
                    onClick = { onEvent(AppsEvent.Restore(app.packageName)) },
                )
            }
        }
    }
}

@Composable
private fun status(app: ConnectedApp): String {
    val last = app.lastBackup
    val detail = last?.detail
    return when {
        !app.included -> stringResource(R.string.apps_not_included)
        last == null -> stringResource(R.string.apps_waiting)
        last.outcome == BackupOutcome.SUCCESS ->
            stringResource(R.string.apps_backed_up, relative(last.finishedAt.toEpochMilli()))
        detail != null -> stringResource(R.string.apps_failed_because, detail)
        else -> stringResource(R.string.apps_failed)
    }
}

@Composable
private fun RestoreDialog(
    message: RestoreMessage?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = message?.let {
        val detail = it.result.detail.orEmpty()
        when (it.result.outcome) {
            AppRestoreOutcome.RESTORED -> stringResource(R.string.apps_restored, it.label)
            AppRestoreOutcome.NOT_APPROVED ->
                stringResource(R.string.apps_restore_not_approved, it.label)
            AppRestoreOutcome.NO_BACKUP -> stringResource(R.string.apps_restore_no_backup, it.label)
            AppRestoreOutcome.NOT_SET_UP -> stringResource(R.string.apps_restore_not_set_up)
            AppRestoreOutcome.FAILED ->
                stringResource(R.string.apps_restore_failed, it.label, detail)
        }
    }
    PvotDialog(
        visible = message != null,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.apps_restore_title),
        message = text,
        modifier = modifier,
    ) {
        PvotDialogAction(label = stringResource(R.string.apps_ok), onClick = onDismiss)
    }
}

private fun relative(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(
            millis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        )
        .toString()
