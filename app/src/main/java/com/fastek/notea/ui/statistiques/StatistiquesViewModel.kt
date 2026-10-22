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

/** Moyenne générale après la [rang]-ième note saisie de la période. */
data class PointGraphique(val rang: Int, val moyenne: Double?)

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

/** Effet d'une matière sur la moyenne générale : (moyenne − moyenne générale) × coefficient ÷ Σ coefficients. */
data class ImpactMatiere(val nom: String, val moyenne: Double, val impact: Double)

data class StatistiquesUiState(
    val chargement: Boolean = true,
    val nomPeriode: String = "",
    val periodeNumero: Int? = null,
    val periodesDisponibles: List<Int> = emptyList(),
    val objectif: Double = 10.0,
    val evolution: List<PointGraphique> = emptyList(),
    val moyennesMatieres: List<ImpactMatiere> = emptyList(),
    val impacts: List<ImpactMatiere> = emptyList(),
    val matieresDisponibles: List<Matiere> = emptyList(),
    val simulateur: SimulateurState = SimulateurState()
)

class StatistiquesViewModel(private val repository: NoteaRepository) : ViewModel() {

    private val _simulateur = MutableStateFlow(SimulateurState())

    /** null = période de la dernière note saisie (sinon la première). */
    private val _periodeChoisie = MutableStateFlow<Int?>(null)

    fun selectionnerPeriode(numero: Int) {
        _periodeChoisie.value = numero
    }

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
                combine(
                    repository.observerPeriodes(profil.id),
                    _periodeChoisie,
                    repository.observerPeriodeDerniereNote()
                ) { periodes, choix, derniere ->
                    Triple(periodes, periodes.find { it.numero == choix }
                        ?: periodes.find { it.id == derniere }
                        ?: periodes.minByOrNull { it.numero }, derniere)
                }.flatMapLatest { (periodes, periode, _) ->
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
                                        construireEtat(
                                            matieresAvecNotes, matieres, periode, simu,
                                            profil.typeEtablissement.nomPeriode, periodes.map { it.numero }.sorted()
                                        )
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
        simuSaisi: SimulateurState,
        nomPeriode: String,
        numerosPeriodes: List<Int>
    ): StatistiquesUiState {
        // Matière présélectionnée : la première matière hors Conduite.
        val simu = if (simuSaisi.matiereId == null) {
            simuSaisi.copy(matiereId = matieres.firstOrNull { it.nom != "Conduite" }?.id ?: matieres.firstOrNull()?.id)
        } else {
            simuSaisi
        }
        val moyennesMatieres = matieresAvecNotes.mapNotNull { (matiere, notes) ->
            MoyenneCalculator.moyenneMatiere(notes)?.let { ImpactMatiere(matiere.nom, it, 0.0) }
        }
        return StatistiquesUiState(
            chargement = false,
            nomPeriode = nomPeriode,
            periodeNumero = periode.numero,
            periodesDisponibles = numerosPeriodes,
            objectif = periode.objectifCible,
            evolution = calculerEvolution(matieresAvecNotes),
            moyennesMatieres = moyennesMatieres,
            impacts = calculerImpacts(matieresAvecNotes),
            matieresDisponibles = matieres,
            simulateur = calculerSimulation(matieresAvecNotes, periode, simu)
                .let { calculerNoteRequise(matieresAvecNotes, periode, it) }
        )
    }

    /**
     * Évolution de la moyenne générale note après note (dans l'ordre de saisie), sur les 30
     * dernières notes : lisible même quand toutes les notes ont été saisies le même jour.
     */
    private fun calculerEvolution(matieresAvecNotes: List<Pair<Matiere, List<Note>>>): List<PointGraphique> {
        val ordonnees = matieresAvecNotes
            .flatMap { (matiere, notes) -> notes.map { matiere to it } }
            .sortedWith(compareBy({ it.second.date }, { it.second.id }))
        val points = ordonnees.indices.map { i ->
            val retenues = ordonnees.take(i + 1).groupBy({ it.first }, { it.second })
            val moyennes = retenues.mapNotNull { (matiere, notes) ->
                MoyenneCalculator.moyenneMatiere(notes)
                    ?.let { MoyenneCalculator.MoyennePonderee(it, matiere.coefficient) }
            }
            PointGraphique(rang = i + 1, moyenne = MoyenneCalculator.moyenneGeneralePeriode(moyennes))
        }
        return points.takeLast(30)
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
            return etat.copy(erreur = "Saisis une note comprise entre 0 et 20.", nouvelleMoyenneMatiere = null, nouvelleMoyenneGenerale = null)
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

    /** Classement des matières notées, de celle qui tire le plus la moyenne vers le bas à celle qui la tire le plus vers le haut. */
    private fun calculerImpacts(matieresAvecNotes: List<Pair<Matiere, List<Note>>>): List<ImpactMatiere> {
        val notees = matieresAvecNotes.mapNotNull { (matiere, notes) ->
            MoyenneCalculator.moyenneMatiere(notes)?.let { matiere to it }
        }
        val totalCoefficients = notees.sumOf { it.first.coefficient }
        if (notees.isEmpty() || totalCoefficients == 0) return emptyList()
        val moyenneGenerale = notees.sumOf { it.second * it.first.coefficient } / totalCoefficients
        return notees
            .map { (matiere, moyenne) ->
                ImpactMatiere(matiere.nom, moyenne, (moyenne - moyenneGenerale) * matiere.coefficient / totalCoefficients)
            }
            .sortedBy { it.impact }
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
