package com.fastek.notea.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator
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
    val moyenneGenerale: Double? = null,
    val objectifPeriode: Double = MoyenneCalculator.OBJECTIF_PAR_DEFAUT,
    val statutGeneral: MoyenneCalculator.StatutObjectif? = null,
    val matieres: List<NoteaRepository.MatiereAvecMoyenne> = emptyList()
)

class DashboardViewModel(repository: NoteaRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(DashboardUiState(chargement = false, profilExiste = false))
            } else {
                repository.observerPeriodes(profil.id).flatMapLatest { periodes ->
                    // La période "courante" = la plus avancée déjà créée.
                    val periodeCourante = periodes.maxByOrNull { it.numero }
                    if (periodeCourante == null) {
                        flowOf(
                            DashboardUiState(
                                chargement = false,
                                profilExiste = true,
                                prenom = profil.prenom
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
}
