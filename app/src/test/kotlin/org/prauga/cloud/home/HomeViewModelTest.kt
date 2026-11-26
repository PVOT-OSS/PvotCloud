// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
    fun showsTheOverviewAndTheRunningBackup() = runTest {
        val overview = BackupOverview.notSetUp()
        val scheduler = Scheduler()
        val progress = RunProgress(files = 3, totalFiles = 10, bytes = 300, totalBytes = 1000)
        val testSubject = HomeViewModel(Repository(flowOf(overview)), scheduler)

        testSubject.state.test {
            assertThat(awaitLoaded()).isEqualTo(UiState.Success(HomeData(overview, running = null)))

            scheduler.progress.value = progress

            assertThat(awaitItem()).isEqualTo(UiState.Success(HomeData(overview, progress)))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backUpNowAsksTheScheduler() {
        val scheduler = Scheduler()
        val testSubject = HomeViewModel(Repository(flowOf(BackupOverview.notSetUp())), scheduler)

        testSubject.onEvent(HomeEvent.BackUpNow)

        assertThat(scheduler.requests).isEqualTo(1)
    }

    @Test
    fun failureShowsAnError() = runTest {
        val testSubject =
            HomeViewModel(Repository(flow { error("storage unavailable") }), Scheduler())

        testSubject.state.test {
            assertThat(awaitLoaded()).isInstanceOf<UiState.Error>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<UiState<HomeData>>.awaitLoaded(): UiState<HomeData> {
        var item = awaitItem()
        while (item == UiState.Loading) item = awaitItem()
        return item
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
