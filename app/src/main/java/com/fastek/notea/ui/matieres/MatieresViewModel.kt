package com.fastek.notea.ui.matieres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.repository.NoteaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MatieresUiState(
    val chargement: Boolean = true,
    val eleveId: Long? = null,
    val periodeId: Long? = null,
    val matieres: List<NoteaRepository.MatiereAvecMoyenne> = emptyList()
)

class MatieresViewModel(repository: NoteaRepository) : ViewModel() {

    val uiState: StateFlow<MatieresUiState> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(MatieresUiState(chargement = false))
            } else {
                repository.observerPeriodes(profil.id).flatMapLatest { periodes ->
                    val periodeCourante = periodes.maxByOrNull { it.numero }
                    if (periodeCourante == null) {
                        flowOf(MatieresUiState(chargement = false, eleveId = profil.id))
                    } else {
                        repository.observerMatieresAvecMoyenne(
                            profil.id,
                            periodeCourante.id,
                            periodeCourante.objectifCible
                        ).map { matieres ->
                            MatieresUiState(
                                chargement = false,
                                eleveId = profil.id,
                                periodeId = periodeCourante.id,
                                matieres = matieres
                            )
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MatieresUiState()
        )
}
