// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.apps

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.prauga.cloud.core.data.ConnectedAppsRepository
import org.prauga.cloud.core.model.AppRestoreOutcome
import org.prauga.cloud.core.model.AppRestoreResult
import org.prauga.cloud.core.model.ConnectedApp

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {

    private val repository = FakeConnectedAppsRepository()
    private lateinit var testSubject: AppsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        testSubject = AppsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun includingAnUnapprovedAppAsksTheAppFirst() = runTest {
        testSubject.effect.test {
            testSubject.onEvent(AppsEvent.SetIncluded(NOTES, included = true))

            val request = ApprovalRequest(NOTES, ApprovalRequest.Then.INCLUDE)
            assertThat(awaitItem()).isEqualTo(AppsEffect.AskApproval(request))
            assertThat(repository.included.value).isEmpty()

            testSubject.onEvent(AppsEvent.Approved(request))
            assertThat(repository.included.value.toList()).containsExactly(NOTES)
        }
    }

    @Test
    fun anApprovedAppIsIncludedAndExcludedDirectly() {
        repository.approved += NOTES

        testSubject.onEvent(AppsEvent.SetIncluded(NOTES, included = true))
        val afterInclude = repository.included.value.toList()
        testSubject.onEvent(AppsEvent.SetIncluded(NOTES, included = false))

        assertThat(afterInclude).containsExactly(NOTES)
        assertThat(repository.included.value).isEmpty()
    }

    @Test
    fun restoringShowsTheResult() = runTest {
        repository.approved += NOTES
        testSubject.state.test {
            skipItems(1)
            testSubject.onEvent(AppsEvent.Restore(NOTES))

            val message = expectMostRecentItem().restored
            assertThat(message)
                .isEqualTo(RestoreMessage("Notes", AppRestoreResult(AppRestoreOutcome.RESTORED)))
            assertThat(repository.restores).containsExactly(NOTES)

            testSubject.onEvent(AppsEvent.DismissMessage)
            assertThat(awaitItem().restored).isEqualTo(null)
        }
    }

    private class FakeConnectedAppsRepository : ConnectedAppsRepository {
        val approved = mutableSetOf<String>()
        val included = MutableStateFlow(emptySet<String>())
        val restores = mutableListOf<String>()

        override val apps: Flow<List<ConnectedApp>> = included.map { set ->
            listOf(ConnectedApp(NOTES, "Notes", NOTES in set, lastBackup = null))
        }

        override fun refresh() = Unit

        override suspend fun isApproved(packageName: String): Boolean = packageName in approved

        override suspend fun include(packageName: String) {
            included.value += packageName
        }

        override suspend fun exclude(packageName: String) {
            included.value -= packageName
        }

        override suspend fun restore(packageName: String): AppRestoreResult {
            restores += packageName
            return AppRestoreResult(AppRestoreOutcome.RESTORED)
        }
    }

    private companion object {
        const val NOTES = "org.example.notes"
    }
}
