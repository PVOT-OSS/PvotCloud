// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.cloud.core.data.ConnectedAppsRepository

@HiltViewModel
class AppsViewModel @Inject constructor(private val repository: ConnectedAppsRepository) :
    ViewModel() {

    private val restoring = MutableStateFlow<String?>(null)
    private val restored = MutableStateFlow<RestoreMessage?>(null)
    private val effects = Channel<AppsEffect>(Channel.BUFFERED)

    val effect: Flow<AppsEffect> = effects.receiveAsFlow()

    val state: StateFlow<AppsUiState> =
        combine(repository.apps, restoring, restored) { apps, working, message ->
                AppsUiState(loading = false, apps = apps, restoring = working, restored = message)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppsUiState())

    fun onEvent(event: AppsEvent) {
        when (event) {
            is AppsEvent.SetIncluded ->
                if (event.included)
                    whenApproved(ApprovalRequest(event.packageName, ApprovalRequest.Then.INCLUDE))
                else viewModelScope.launch { repository.exclude(event.packageName) }
            is AppsEvent.Restore ->
                whenApproved(ApprovalRequest(event.packageName, ApprovalRequest.Then.RESTORE))
            is AppsEvent.Approved -> viewModelScope.launch { carryOut(event.request) }
            AppsEvent.Refresh -> repository.refresh()
            AppsEvent.DismissMessage -> restored.value = null
        }
    }

    private fun whenApproved(request: ApprovalRequest) {
        viewModelScope.launch {
            if (repository.isApproved(request.packageName)) carryOut(request)
            else effects.send(AppsEffect.AskApproval(request))
        }
    }

    private suspend fun carryOut(request: ApprovalRequest) {
        when (request.then) {
            ApprovalRequest.Then.INCLUDE -> repository.include(request.packageName)
            ApprovalRequest.Then.RESTORE -> restore(request.packageName)
        }
    }

    private suspend fun restore(packageName: String) {
        if (restoring.value != null) return
        restoring.value = packageName
        val label =
            state.value.apps.firstOrNull { it.packageName == packageName }?.label ?: packageName
        val result = repository.restore(packageName)
        restoring.value = null
        restored.value = RestoreMessage(label, result)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
