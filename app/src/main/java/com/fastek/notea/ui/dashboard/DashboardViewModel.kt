package com.fastek.notea.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.data.repository.PeriodeCourante
import com.fastek.notea.domain.calcul.MoyenneCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val chargement: Boolean = true,
    val profilExiste: Boolean = false,
    val prenom: String = "",
    val nomPeriode: String = "Semestre",
    val periodeNumero: Int? = null,
    val periodeId: Long? = null,
    val periodeTerminee: Boolean = false,
    val periodesDisponibles: List<Int> = emptyList(),
    val moyenneGenerale: Double? = null,
    val objectifPeriode: Double = MoyenneCalculator.OBJECTIF_PAR_DEFAUT,
    val statutGeneral: MoyenneCalculator.StatutObjectif? = null,
    val matieres: List<NoteaRepository.MatiereAvecMoyenne> = emptyList()
)

class DashboardViewModel(private val repository: NoteaRepository) : ViewModel() {

    /** null = pas de choix explicite -> période de la dernière note saisie (sinon la première). */
    private val _periodeChoisie = repository.periodeChoisie

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
                        val periodeCourante = PeriodeCourante.choisir(periodes, choix)

                        if (periodeCourante == null) {
                            flowOf(
                                DashboardUiState(
                                    chargement = false,
                                    profilExiste = true,
                                    prenom = profil.prenom,
                                    nomPeriode = profil.typeEtablissement.nomPeriode,
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
                                    nomPeriode = profil.typeEtablissement.nomPeriode,
                                    periodeNumero = periodeCourante.numero,
                                    periodeId = periodeCourante.id,
                                    periodeTerminee = periodeCourante.terminee,
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

    /** Marque la période affichée comme terminée (ou non) : l'affichage par défaut passe à la suivante. */
    fun definirPeriodeTerminee(periodeId: Long, terminee: Boolean) {
        viewModelScope.launch { repository.definirPeriodeTerminee(periodeId, terminee) }
    }
}
