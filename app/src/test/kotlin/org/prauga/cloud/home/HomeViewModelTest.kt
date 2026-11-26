// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.home

import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.prauga.cloud.core.data.BackupScheduler
import org.prauga.cloud.core.data.BackupStatusRepository
import org.prauga.cloud.core.model.BackupOverview
import org.prauga.cloud.core.model.RunProgress

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheOverview() = runTest {
        val overview = BackupOverview.notSetUp()
        val scheduler = Scheduler()
        val viewModel = HomeViewModel(Repository(flowOf(overview)), scheduler)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        assertEquals(UiState.Success(HomeData(overview, running = null)), viewModel.state.value)

        val progress = RunProgress(files = 3, totalFiles = 10, bytes = 300, totalBytes = 1000)
        scheduler.progress.value = progress
        assertEquals(UiState.Success(HomeData(overview, progress)), viewModel.state.value)

        viewModel.onEvent(HomeEvent.BackUpNow)
        assertEquals(1, scheduler.requests)
    }

    @Test
    fun failureShowsAnError() = runTest {
        val viewModel =
            HomeViewModel(Repository(flow { error("storage unavailable") }), Scheduler())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        assertTrue(viewModel.state.value is UiState.Error)
    }

    private class Repository(override val overview: Flow<BackupOverview>) : BackupStatusRepository

    private class Scheduler : BackupScheduler {
        val progress = MutableStateFlow<RunProgress?>(null)
        var requests = 0
        override val running: Flow<RunProgress?> = progress

        override fun backUpNow() {
            requests++
        }
    }
}
