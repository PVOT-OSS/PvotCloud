// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.cloud.core.data.BackupSetupRepository
import org.prauga.cloud.core.keys.RecoveryPhrase
import org.prauga.cloud.core.keys.RecoveryPhraseStore

@HiltViewModel
class OnboardingViewModel
internal constructor(
    private val setup: BackupSetupRepository,
    private val phraseStore: RecoveryPhraseStore,
    private val random: Random,
) : ViewModel() {

    @Inject
    constructor(
        setup: BackupSetupRepository,
        phraseStore: RecoveryPhraseStore,
    ) : this(setup, phraseStore, Random.Default)

    private val mutableState = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Start -> mutableState.value = OnboardingUiState()
            OnboardingEvent.Continue ->
                mutableState.update { it.copy(step = OnboardingStep.LOCATION) }
            is OnboardingEvent.LocationPicked -> pickLocation(event.treeUri)
            OnboardingEvent.PhraseWritten ->
                mutableState.update {
                    it.copy(
                        step = OnboardingStep.CONFIRM,
                        questions = ConfirmQuestion.create(it.words, random),
                        answers = emptyMap(),
                        answersWrong = false,
                    )
                }
            is OnboardingEvent.Answer ->
                mutableState.update {
                    it.copy(
                        answers = it.answers + (event.position to event.word),
                        answersWrong = false,
                    )
                }
            OnboardingEvent.CheckAnswers -> checkAnswers()
            OnboardingEvent.ShowPhraseAgain ->
                mutableState.update { it.copy(step = OnboardingStep.PHRASE) }
            is OnboardingEvent.SetCategory ->
                mutableState.update {
                    it.copy(
                        categories =
                            if (event.enabled) it.categories + event.category
                            else it.categories - event.category
                    )
                }
            OnboardingEvent.SaveCategories -> saveCategories()
        }
    }

    private fun pickLocation(treeUri: String?) {
        if (treeUri == null) return
        mutableState.update { it.copy(busy = true, locationFailed = false) }
        viewModelScope.launch {
            val location = setup.chooseLocation(treeUri)
            mutableState.update {
                if (location == null) {
                    it.copy(busy = false, locationFailed = true)
                } else {
                    it.copy(
                        busy = false,
                        locationName = location.name,
                        step = OnboardingStep.PHRASE,
                        words = RecoveryPhrase.generate(),
                    )
                }
            }
        }
    }

    private fun checkAnswers() {
        val current = mutableState.value
        val correct =
            current.questions.all { current.answers[it.position] == current.words[it.position] }
        if (!correct) {
            mutableState.update { it.copy(answersWrong = true, answers = emptyMap()) }
            return
        }
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            phraseStore.save(current.words)
            setup.confirmPhrase()
            mutableState.update {
                it.copy(
                    busy = false,
                    step = OnboardingStep.CATEGORIES,
                    words = emptyList(),
                    questions = emptyList(),
                    answers = emptyMap(),
                )
            }
        }
    }

    private fun saveCategories() {
        val categories = mutableState.value.categories
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            setup.setCategories(categories)
            mutableState.update { it.copy(busy = false, step = OnboardingStep.DONE) }
        }
    }
}
