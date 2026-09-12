package com.fastek.notea.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val chargement: Boolean = true,
    val profilExiste: Boolean = false,
    val prenom: String = "",
    val periodeNumero: Int? = null,
    val periodesDisponibles: List<Int> = emptyList(),
    val moyenneGenerale: Double? = null,
    val objectifPeriode: Double = MoyenneCalculator.OBJECTIF_PAR_DEFAUT,
    val statutGeneral: MoyenneCalculator.StatutObjectif? = null,
    val matieres: List<NoteaRepository.MatiereAvecMoyenne> = emptyList()
)

class DashboardViewModel(repository: NoteaRepository) : ViewModel() {

    /** null = pas de choix explicite -> on affiche la période la plus avancée. */
    private val _periodeChoisie = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<DashboardUiState> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(DashboardUiState(chargement = false, profilExiste = false))
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
                                DashboardUiState(
                                    chargement = false,
                                    profilExiste = true,
                                    prenom = profil.prenom,
                                    periodesDisponibles = periodes.map { it.numero }
                                )
                            )
                        } else {
                            combine(
                                repository.observerMatieresAvecMoyenne(
                                    profil.id,
                                    periodeCourante.id,
                                    periodeCourante.objectifCible
                                ),
                                repository.observerMoyenneGeneralePeriode(profil.id, periodeCourante)
                            ) { matieres, moyenneGenerale ->
                                DashboardUiState(
                                    chargement = false,
                                    profilExiste = true,
                                    prenom = profil.prenom,
                                    periodeNumero = periodeCourante.numero,
                                    periodesDisponibles = periodes.map { it.numero },
                                    moyenneGenerale = moyenneGenerale,
                                    objectifPeriode = periodeCourante.objectifCible,
                                    statutGeneral = moyenneGenerale?.let {
                                        MoyenneCalculator.statutObjectif(it, periodeCourante.objectifCible)
                                    },
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
            initialValue = DashboardUiState()
        )

    fun selectionnerPeriode(numero: Int) {
        _periodeChoisie.value = numero
    }
}
