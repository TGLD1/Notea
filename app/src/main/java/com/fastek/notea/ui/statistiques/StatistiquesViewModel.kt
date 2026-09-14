package com.fastek.notea.ui.statistiques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.Periode
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class PointGraphique(val jour: Int, val moyenne: Double?)

data class SimulateurState(
    val matiereId: Long? = null,
    val type: TypeNote = TypeNote.INTERROGATION,
    val valeurTexte: String = "",
    val nouvelleMoyenneMatiere: Double? = null,
    val nouvelleMoyenneGenerale: Double? = null,
    val erreur: String? = null
)

data class StatistiquesUiState(
    val chargement: Boolean = true,
    val evolution: List<PointGraphique> = emptyList(),
    val matieresDisponibles: List<Matiere> = emptyList(),
    val simulateur: SimulateurState = SimulateurState()
)

class StatistiquesViewModel(private val repository: NoteaRepository) : ViewModel() {

    private val _simulateur = MutableStateFlow(SimulateurState())

    val uiState: StateFlow<StatistiquesUiState> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(StatistiquesUiState(chargement = false))
            } else {
                repository.observerPeriodes(profil.id).flatMapLatest { periodes ->
                    val periode = periodes.maxByOrNull { it.numero }
                    if (periode == null) {
                        flowOf(StatistiquesUiState(chargement = false))
                    } else {
                        repository.observerMatieres(profil.id).flatMapLatest { matieres ->
                            if (matieres.isEmpty()) {
                                flowOf(StatistiquesUiState(chargement = false))
                            } else {
                                combine(
                                    matieres.map { matiere ->
                                        repository.observerNotes(matiere.id, periode.id)
                                            .map { notes -> matiere to notes }
                                    }
                                ) { paires -> paires.toList() }
                                    .combine(_simulateur) { matieresAvecNotes, simu ->
                                        construireEtat(matieresAvecNotes, matieres, periode, simu)
                                    }
                            }
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatistiquesUiState())

    private fun construireEtat(
        matieresAvecNotes: List<Pair<Matiere, List<Note>>>,
        matieres: List<Matiere>,
        periode: Periode,
        simu: SimulateurState
    ): StatistiquesUiState {
        val aujourdHui = LocalDate.now()
        val evolution = (0..29).map { i ->
            val jourDate = aujourdHui.minusDays((29 - i).toLong())
            PointGraphique(
                jour = i,
                moyenne = MoyenneCalculator.moyenneGeneraleADate(matieresAvecNotes, jourDate, periode.noteConduite)
            )
        }

        return StatistiquesUiState(
            chargement = false,
            evolution = evolution,
            matieresDisponibles = matieres,
            simulateur = calculerSimulation(matieresAvecNotes, periode, simu)
        )
    }

    private fun calculerSimulation(
        matieresAvecNotes: List<Pair<Matiere, List<Note>>>,
        periode: Periode,
        etat: SimulateurState
    ): SimulateurState {
        val matiereChoisie = matieresAvecNotes.find { it.first.id == etat.matiereId }
            ?: return etat.copy(nouvelleMoyenneMatiere = null, nouvelleMoyenneGenerale = null, erreur = null)

        val valeur = etat.valeurTexte.replace(',', '.').toDoubleOrNull()
            ?: return etat.copy(nouvelleMoyenneMatiere = null, nouvelleMoyenneGenerale = null, erreur = null)

        if (valeur < 0.0 || valeur > 20.0) {
            return etat.copy(erreur = "Note invalide (0 à 20)", nouvelleMoyenneMatiere = null, nouvelleMoyenneGenerale = null)
        }

        val noteHypothetique = Note(
            matiereId = matiereChoisie.first.id,
            periodeId = periode.id,
            type = etat.type,
            valeur = valeur,
            date = LocalDate.now()
        )
        val notesHypothetiques = matiereChoisie.second + noteHypothetique
        val nouvelleMoyenneMatiere = MoyenneCalculator.moyenneMatiere(notesHypothetiques)

        val moyennesPonderees = matieresAvecNotes.mapNotNull { (matiere, notes) ->
            val notesAUtiliser = if (matiere.id == matiereChoisie.first.id) notesHypothetiques else notes
            MoyenneCalculator.moyenneMatiere(notesAUtiliser)
                ?.let { MoyenneCalculator.MoyennePonderee(it, matiere.coefficient) }
        }
        val nouvelleMoyenneGenerale = MoyenneCalculator.moyenneGeneralePeriode(moyennesPonderees, periode.noteConduite)

        return etat.copy(
            nouvelleMoyenneMatiere = nouvelleMoyenneMatiere,
            nouvelleMoyenneGenerale = nouvelleMoyenneGenerale,
            erreur = null
        )
    }

    fun onMatiereChange(matiereId: Long?) = _simulateur.update {
        it.copy(matiereId = matiereId, valeurTexte = "", nouvelleMoyenneMatiere = null, nouvelleMoyenneGenerale = null, erreur = null)
    }

    fun onTypeChange(type: TypeNote) = _simulateur.update { it.copy(type = type) }

    fun onValeurChange(valeur: String) {
        if (valeur.isEmpty() || valeur.matches(Regex("^\\d{0,2}([.,]\\d{0,2})?$"))) {
            _simulateur.update { it.copy(valeurTexte = valeur) }
        }
    }
}
