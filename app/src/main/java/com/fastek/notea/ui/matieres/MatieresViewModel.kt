package com.fastek.notea.ui.matieres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.repository.NoteaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MatieresUiState(
    val chargement: Boolean = true,
    val eleveId: Long? = null,
    val periodeId: Long? = null,
    val periodeNumero: Int? = null,
    val periodesDisponibles: List<Int> = emptyList(),
    val matieres: List<NoteaRepository.MatiereAvecMoyenne> = emptyList()
)

class MatieresViewModel(repository: NoteaRepository) : ViewModel() {

    private val _periodeChoisie = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<MatieresUiState> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(MatieresUiState(chargement = false))
            } else {
                combine(
                    repository.observerPeriodes(profil.id),
                    _periodeChoisie
                ) { periodes, choix -> periodes to choix }
                    .flatMapLatest { (periodes, choix) ->
                        val periodeCourante = periodes.find { it.numero == choix }
                            ?: periodes.maxByOrNull { it.numero }

                        if (periodeCourante == null) {
                            flowOf(
                                MatieresUiState(
                                    chargement = false,
                                    eleveId = profil.id,
                                    periodesDisponibles = periodes.map { it.numero }
                                )
                            )
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
                                    periodeNumero = periodeCourante.numero,
                                    periodesDisponibles = periodes.map { it.numero },
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

    fun selectionnerPeriode(numero: Int) {
        _periodeChoisie.value = numero
    }
}
