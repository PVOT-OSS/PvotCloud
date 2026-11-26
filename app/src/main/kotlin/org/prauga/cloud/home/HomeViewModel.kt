// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.cloud.core.data.BackupScheduler
import org.prauga.cloud.core.data.BackupStatusRepository

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val repository: BackupStatusRepository,
    private val scheduler: BackupScheduler,
) : ViewModel() {
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState<HomeData>> =
        attempt
            .flatMapLatest {
                combine(repository.overview, scheduler.running, ::HomeData)
                    .map<HomeData, UiState<HomeData>> { UiState.Success(it) }
                    .catch { emit(UiState.Error(it.message.orEmpty(), it)) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Retry -> attempt.value++
            HomeEvent.BackUpNow -> scheduler.backUpNow()
        }
    }
}
