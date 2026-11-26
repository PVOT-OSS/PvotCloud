// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
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
    private val testSubject = OnboardingViewModel(setup, store, Random(7))
    private val state
        get() = testSubject.state.value

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
            testSubject.onEvent(OnboardingEvent.Answer(question.position, word))
        }
    }

    @Test
    fun choosingAFolderShowsANewPhrase() {
        testSubject.onEvent(OnboardingEvent.Continue)
        testSubject.onEvent(OnboardingEvent.LocationPicked(null))
        assertThat(state.step).isEqualTo(OnboardingStep.LOCATION)

        setup.refuse = true
        testSubject.onEvent(OnboardingEvent.LocationPicked("content://tree/denied"))
        assertThat(state.locationFailed).isTrue()

        setup.refuse = false
        testSubject.onEvent(OnboardingEvent.LocationPicked("content://tree/sandisk"))
        assertThat(state.step).isEqualTo(OnboardingStep.PHRASE)
        assertThat(state.locationName).isEqualTo("SanDisk")
        assertThat(RecoveryPhrase.isValid(state.words)).isTrue()
    }

    @Test
    fun wrongAnswersAreRejectedAndRightOnesSaveThePhrase() {
        testSubject.onEvent(OnboardingEvent.Continue)
        testSubject.onEvent(OnboardingEvent.LocationPicked("content://tree/sandisk"))
        val words = state.words
        testSubject.onEvent(OnboardingEvent.PhraseWritten)
        assertThat(state.questions).hasSize(3)
        state.questions.forEach { assertThat(it.options).contains(words[it.position]) }

        answer(correct = false)
        testSubject.onEvent(OnboardingEvent.CheckAnswers)
        assertThat(state.answersWrong).isTrue()
        assertThat(state.step).isEqualTo(OnboardingStep.CONFIRM)
        assertThat(store.saved).isNull()
        assertThat(state.allAnswered).isFalse()

        answer(correct = true)
        testSubject.onEvent(OnboardingEvent.CheckAnswers)
        assertThat(state.step).isEqualTo(OnboardingStep.CATEGORIES)
        assertThat(store.saved).isEqualTo(words)
        assertThat(setup.stored.value.phraseConfirmed).isTrue()
        assertThat(state.words).isEmpty()
    }

    @Test
    fun chosenCategoriesAreSaved() {
        testSubject.onEvent(OnboardingEvent.SetCategory(BackupCategory.MESSAGES, enabled = false))
        testSubject.onEvent(OnboardingEvent.SaveCategories)

        assertThat(state.step).isEqualTo(OnboardingStep.DONE)
        assertThat(setup.stored.value.categories)
            .isEqualTo(BackupCategory.entries.toSet() - BackupCategory.MESSAGES)
    }

    @Test
    fun questionsAskThreeDifferentPositionsWithFourChoices() {
        val words = RecoveryPhrase.generate()

        val questions = ConfirmQuestion.create(words, Random(1))

        assertThat(questions.map { it.position }.distinct()).hasSize(3)
        questions.forEach {
            assertThat(it.options.distinct()).hasSize(4)
            assertThat(it.options).contains(words[it.position])
        }
    }
}
