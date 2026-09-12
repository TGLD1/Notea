package com.fastek.notea.ui.parametres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.Eleve
import com.fastek.notea.data.repository.NoteaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfilViewModel(repository: NoteaRepository) : ViewModel() {
    val profil: StateFlow<Eleve?> = repository.observerProfil()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
