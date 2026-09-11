package com.fastek.notea.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.ui.dashboard.DashboardViewModel
import com.fastek.notea.ui.matieres.MatieresViewModel
import com.fastek.notea.ui.objectifs.ObjectifsViewModel
import com.fastek.notea.ui.onboarding.OnboardingViewModel

class NoteaViewModelFactory(
    private val repository: NoteaRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
            OnboardingViewModel(repository) as T
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(repository) as T
        modelClass.isAssignableFrom(MatieresViewModel::class.java) ->
            MatieresViewModel(repository) as T
        modelClass.isAssignableFrom(ObjectifsViewModel::class.java) ->
            ObjectifsViewModel(repository) as T
        else -> throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
    }
}
