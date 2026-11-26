// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.prauga.cloud.core.data.BackupSetupRepository
import org.prauga.cloud.core.keys.RecoveryPhrase
import org.prauga.cloud.core.keys.RecoveryPhraseStore
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.SavedLocation

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private class FakeSetup : BackupSetupRepository {
        val stored = MutableStateFlow(BackupSettings())
        var refuse = false
        override val settings: Flow<BackupSettings> = stored

        override suspend fun chooseLocation(treeUri: String): SavedLocation? =
            if (refuse) null
            else
                SavedLocation(treeUri, "SanDisk").also {
                    stored.value = stored.value.copy(location = it)
                }

        override suspend fun setCategories(categories: Set<BackupCategory>) {
            stored.value = stored.value.copy(categories = categories)
        }

        override suspend fun confirmPhrase() {
            stored.value = stored.value.copy(phraseConfirmed = true)
        }
    }

    private class FakeStore : RecoveryPhraseStore {
        var saved: List<String>? = null

        override suspend fun save(words: List<String>) {
            saved = words
        }

        override suspend fun load(): List<String>? = saved

        override suspend fun exists(): Boolean = saved != null
    }

    private val setup = FakeSetup()
    private val store = FakeStore()
    private val viewModel = OnboardingViewModel(setup, store, Random(7))
    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun answer(correct: Boolean) {
        state.questions.forEach { question ->
            val right = state.words[question.position]
            val word = if (correct) right else question.options.first { it != right }
            viewModel.onEvent(OnboardingEvent.Answer(question.position, word))
        }
    }

    @Test
    fun choosingAFolderShowsANewPhrase() {
        viewModel.onEvent(OnboardingEvent.Continue)
        viewModel.onEvent(OnboardingEvent.LocationPicked(null))
        assertEquals(OnboardingStep.LOCATION, state.step)

        setup.refuse = true
        viewModel.onEvent(OnboardingEvent.LocationPicked("content://tree/denied"))
        assertTrue(state.locationFailed)

        setup.refuse = false
        viewModel.onEvent(OnboardingEvent.LocationPicked("content://tree/sandisk"))
        assertEquals(OnboardingStep.PHRASE, state.step)
        assertEquals("SanDisk", state.locationName)
        assertTrue(RecoveryPhrase.isValid(state.words))
    }

    @Test
    fun wrongAnswersAreRejectedAndRightOnesSaveThePhrase() {
        viewModel.onEvent(OnboardingEvent.Continue)
        viewModel.onEvent(OnboardingEvent.LocationPicked("content://tree/sandisk"))
        val words = state.words
        viewModel.onEvent(OnboardingEvent.PhraseWritten)
        assertEquals(3, state.questions.size)
        state.questions.forEach { assertTrue(words[it.position] in it.options) }

        answer(correct = false)
        viewModel.onEvent(OnboardingEvent.CheckAnswers)
        assertTrue(state.answersWrong)
        assertEquals(OnboardingStep.CONFIRM, state.step)
        assertNull(store.saved)
        assertFalse(state.allAnswered)

        answer(correct = true)
        viewModel.onEvent(OnboardingEvent.CheckAnswers)
        assertEquals(OnboardingStep.CATEGORIES, state.step)
        assertEquals(words, store.saved)
        assertTrue(setup.stored.value.phraseConfirmed)
        assertTrue(state.words.isEmpty())
    }

    @Test
    fun chosenCategoriesAreSaved() {
        viewModel.onEvent(OnboardingEvent.SetCategory(BackupCategory.MESSAGES, enabled = false))
        viewModel.onEvent(OnboardingEvent.SaveCategories)

        assertEquals(OnboardingStep.DONE, state.step)
        assertEquals(
            BackupCategory.entries.toSet() - BackupCategory.MESSAGES,
            setup.stored.value.categories,
        )
    }

    @Test
    fun questionsAskThreeDifferentPositionsWithFourChoices() {
        val words = RecoveryPhrase.generate()

        val questions = ConfirmQuestion.create(words, Random(1))

        assertEquals(3, questions.map { it.position }.distinct().size)
        questions.forEach {
            assertEquals(4, it.options.distinct().size)
            assertTrue(words[it.position] in it.options)
        }
    }
}
