package com.fastek.notea.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.ui.dashboard.DashboardViewModel
import com.fastek.notea.ui.onboarding.OnboardingViewModel

/**
 * Pas de framework d'injection (Hilt/Koin) volontairement — l'app reste petite
 * et 100% locale, une factory manuelle suffit et évite du poids/complexité.
 */
class NoteaViewModelFactory(
    private val repository: NoteaRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
            OnboardingViewModel(repository) as T
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(repository) as T
        else -> throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
    }
}
