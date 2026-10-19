package com.fastek.notea.ui.statistiques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.Periode
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator
import com.fastek.notea.ui.notes.NotesViewModel
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
    val erreur: String? = null,
    /** Moyenne générale visée (texte saisi ; vide = objectif de la période). */
    val cibleTexte: String = "",
    val cible: Double? = null,
    /** Note minimale requise pour atteindre [cible] (calcul inverse). */
    val requis: MoyenneCalculator.Requis? = null,
    val limiteDevoirs: Boolean = false
)

/** Une ligne du calculateur de passage : cible de moyenne annuelle et moyenne requise à la dernière période. */
data class PassageResultat(val cible: Double, val requis: MoyenneCalculator.Requis)

data class PassageUi(
    val libelleDernierePeriode: String,
    val libellePeriodePrecedente: String,
    /** false tant qu'une période précédente n'a aucune note. */
    val disponible: Boolean,
    val resultats: List<PassageResultat> = emptyList()
)

data class StatistiquesUiState(
    val chargement: Boolean = true,
    val evolution: List<PointGraphique> = emptyList(),
    val matieresDisponibles: List<Matiere> = emptyList(),
    val simulateur: SimulateurState = SimulateurState()
)

class StatistiquesViewModel(private val repository: NoteaRepository) : ViewModel() {

    private val _simulateur = MutableStateFlow(SimulateurState())

    /** Calculateur de passage : moyenne requise à la dernière période pour une moyenne annuelle visée. */
    val passage: StateFlow<PassageUi?> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(null)
            } else {
                repository.observerPeriodes(profil.id).flatMapLatest { periodes ->
                    val type = profil.typeEtablissement
                    val nom = type.nomPeriode
                    val precedentes = periodes.filter { it.numero < type.nombrePeriodes }.sortedBy { it.numero }
                    if (precedentes.size != type.nombrePeriodes - 1) {
                        flowOf(null)
                    } else {
                        combine(precedentes.map { repository.observerMoyenneGeneralePeriode(profil.id, it) }) { moyennes ->
                            val libelleFinal = "$nom ${type.nombrePeriodes}"
                            val libellePrecedent = if (precedentes.size == 1) "$nom 1" else "les ${nom.lowercase()}s précédents"
                            if (moyennes.any { it == null }) {
                                PassageUi(libelleFinal, libellePrecedent, disponible = false)
                            } else {
                                val connues = moyennes.map { it!! }
                                val cibles = (listOf(10.0, 12.0, profil.objectifAnnuel)).distinct()
                                PassageUi(
                                    libelleDernierePeriode = libelleFinal,
                                    libellePeriodePrecedente = libellePrecedent,
                                    disponible = true,
                                    resultats = cibles.map { cible ->
                                        PassageResultat(cible, MoyenneCalculator.moyenneRequiseDernierePeriode(type, connues, cible))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
                moyenne = MoyenneCalculator.moyenneGeneraleADate(matieresAvecNotes, jourDate)
            )
        }

        return StatistiquesUiState(
            chargement = false,
            evolution = evolution,
            matieresDisponibles = matieres,
            simulateur = calculerSimulation(matieresAvecNotes, periode, simu)
                .let { calculerNoteRequise(matieresAvecNotes, periode, it) }
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
        val nouvelleMoyenneGenerale = MoyenneCalculator.moyenneGeneralePeriode(moyennesPonderees)

        return etat.copy(
            nouvelleMoyenneMatiere = nouvelleMoyenneMatiere,
            nouvelleMoyenneGenerale = nouvelleMoyenneGenerale,
            erreur = null
        )
    }

    /** Calcul inverse : note minimale pour atteindre la moyenne générale visée. */
    private fun calculerNoteRequise(
        matieresAvecNotes: List<Pair<Matiere, List<Note>>>,
        periode: Periode,
        etat: SimulateurState
    ): SimulateurState {
        val cible = etat.cibleTexte.replace(',', '.').toDoubleOrNull() ?: periode.objectifCible
        val choisie = matieresAvecNotes.find { it.first.id == etat.matiereId }
            ?: return etat.copy(cible = cible, requis = null, limiteDevoirs = false)
        if (etat.type == TypeNote.DEVOIR &&
            choisie.second.count { it.type == TypeNote.DEVOIR } >= NotesViewModel.MAX_DEVOIRS_PAR_PERIODE
        ) {
            return etat.copy(cible = cible, requis = null, limiteDevoirs = true)
        }
        val autres = matieresAvecNotes
            .filter { it.first.id != choisie.first.id }
            .mapNotNull { (matiere, notes) ->
                MoyenneCalculator.moyenneMatiere(notes)
                    ?.let { MoyenneCalculator.MoyennePonderee(it, matiere.coefficient) }
            }
        val requis = MoyenneCalculator.noteRequisePourMoyenneGenerale(
            choisie.second, etat.type, choisie.first.coefficient, autres, cible
        )
        return etat.copy(cible = cible, requis = requis, limiteDevoirs = false)
    }

    fun onCibleChange(texte: String) {
        if (texte.isEmpty() || texte.matches(Regex("^\\d{0,2}([.,]\\d{0,2})?$"))) {
            _simulateur.update { it.copy(cibleTexte = texte) }
        }
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
