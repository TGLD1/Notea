package com.fastek.notea.enseignant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Creneau
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import com.fastek.notea.enseignant.data.entity.Professeur
import com.fastek.notea.enseignant.data.repository.EnseignantRepository
import com.fastek.notea.enseignant.domain.ApercuClasse
import com.fastek.notea.enseignant.domain.ClasseCalculator
import com.fastek.notea.enseignant.domain.EleveNotes
import com.fastek.notea.enseignant.domain.ListeElevesParser
import com.fastek.notea.enseignant.domain.NoteSaisie
import com.fastek.notea.enseignant.domain.calculerApercu
import com.fastek.notea.enseignant.domain.ResultatPeriode
import com.fastek.notea.enseignant.domain.LigneResultat
import com.fastek.notea.enseignant.export.ClasseurExport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EtatProfesseur {
    data object Chargement : EtatProfesseur
    data object Absent : EtatProfesseur
    data class Present(val professeur: Professeur) : EtatProfesseur
}

private fun versEtat(professeur: Professeur?): EtatProfesseur =
    if (professeur == null) EtatProfesseur.Absent else EtatProfesseur.Present(professeur)

/** Profil du professeur et liste de ses classes (écran d'accueil). */
class EnseignantViewModel(private val repo: EnseignantRepository) : ViewModel() {

    val etat: StateFlow<EtatProfesseur> = repo.observerProfesseur()
        .map(::versEtat)
        .stateIn(viewModelScope, SharingStarted.Eagerly, EtatProfesseur.Chargement)

    @OptIn(ExperimentalCoroutinesApi::class)
    val classes: StateFlow<List<Classe>> = etat
        .flatMapLatest { e ->
            if (e is EtatProfesseur.Present) repo.observerClasses(e.professeur.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creneaux: StateFlow<List<Creneau>> = repo.observerCreneaux()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalEleves: StateFlow<Int> = repo.observerNombreEleves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun professeurActuel(): Professeur? = (etat.value as? EtatProfesseur.Present)?.professeur

    /** Dernier message d'erreur à montrer à l'écran (null s'il n'y en a pas). */
    val erreur = MutableStateFlow<String?>(null)

    fun effacerErreur() {
        erreur.value = null
    }

    /** Exécute une action en base ; en cas d'échec, l'erreur est écrite au journal et montrée à l'écran. */
    private fun executer(action: String, bloc: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                bloc()
                Journal.ok(action)
            } catch (e: Exception) {
                Journal.erreur(action, e)
                erreur.value = "Action impossible (${e.message ?: e.javaClass.simpleName})"
            }
        }
    }

    fun creerProfesseur(
        nom: String, prenom: String, matiere: String,
        type: TypeEtablissement, anneeScolaire: String, etablissement: String
    ) = executer("profil.creation") {
        repo.creerProfesseur(
            Professeur(
                nom = nom.trim(), prenom = prenom.trim(), matiere = matiere.trim(),
                typeEtablissement = type, anneeScolaire = anneeScolaire.trim(),
                etablissement = etablissement.trim()
            )
        )
    }

    fun mettreAJourProfesseur(professeur: Professeur) = executer("profil.modification") {
        repo.mettreAJourProfesseur(professeur)
    }

    fun creerClasse(nom: String, coefficient: Int) = executer("classe.creation") {
        repo.creerClasseDuProfesseur(nom, coefficient)
    }

    fun modifierClasse(classe: Classe, nom: String, coefficient: Int) = executer("classe.modification") {
        repo.mettreAJourClasse(classe.copy(nom = nom.trim(), coefficient = coefficient))
    }

    fun supprimerClasse(classe: Classe) = executer("classe.suppression") {
        repo.supprimerClasse(classe)
    }

    fun effectif(classeId: Long): Flow<Int> = repo.observerInscrits(classeId).map { it.size }

    /** Effectif, moyenne de classe et avancement de la saisie d'une classe, mis à jour en direct. */
    fun apercu(classeId: Long): Flow<ApercuClasse> = combine(
        repo.observerInscrits(classeId),
        repo.observerPeriodes(classeId),
        repo.observerEvaluationsClasse(classeId),
        repo.observerNotesClasse(classeId)
    ) { inscrits, periodes, evaluations, notes -> calculerApercu(inscrits, periodes, evaluations, notes) }

    suspend fun preparerExport(): ClasseurExport? {
        val professeur = professeurActuel() ?: return null
        return repo.preparerExport(professeur)
    }
}

data class LigneUi(val inscrit: Inscrit, val ligne: LigneResultat)

data class ResultatsUi(val periode: ResultatPeriode, val lignes: List<LigneUi>) {
    companion object {
        val VIDE = ResultatsUi(ResultatPeriode(emptyList(), ClasseCalculator.statistiques(emptyList())), emptyList())
    }
}

/** Une classe : élèves, évaluations de la période choisie, notes et résultats. */
class ClasseViewModel(private val repo: EnseignantRepository, private val classeId: Long) : ViewModel() {

    private val partage = SharingStarted.WhileSubscribed(5000)

    val classe = MutableStateFlow<Classe?>(null)

    init {
        viewModelScope.launch { classe.value = repo.obtenirClasse(classeId) }
    }

    val professeur: StateFlow<Professeur?> = repo.observerProfesseur().stateIn(viewModelScope, partage, null)

    val periodes: StateFlow<List<PeriodeClasse>> =
        repo.observerPeriodes(classeId).stateIn(viewModelScope, partage, emptyList())

    val inscrits: StateFlow<List<Inscrit>> =
        repo.observerInscrits(classeId).stateIn(viewModelScope, partage, emptyList())

    private val numeroChoisi = MutableStateFlow(1)
    val numero: StateFlow<Int> = numeroChoisi

    fun choisirPeriode(numero: Int) {
        numeroChoisi.value = numero
    }

    private val periodeCourante: StateFlow<PeriodeClasse?> =
        combine(periodes, numeroChoisi) { liste, n -> liste.firstOrNull { it.numero == n } ?: liste.firstOrNull() }
            .stateIn(viewModelScope, partage, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val evaluations: StateFlow<List<Evaluation>> = periodeCourante
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else repo.observerEvaluations(p.id) }
        .stateIn(viewModelScope, partage, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEleve>> = periodeCourante
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else repo.observerNotes(p.id) }
        .stateIn(viewModelScope, partage, emptyList())

    val resultats: StateFlow<ResultatsUi> = combine(inscrits, evaluations, notes) { eleves, evals, valeurs ->
        val parCle = valeurs.associate { (it.evaluationId to it.inscritId) to it.valeur }
        val entrees = eleves.map { e ->
            EleveNotes(
                e.id,
                evals.mapNotNull { ev -> parCle[ev.id to e.id]?.let { NoteSaisie(ev.type, it) } }
            )
        }
        val periode = ClasseCalculator.calculerPeriode(entrees)
        ResultatsUi(periode, eleves.zip(periode.lignes).map { (e, l) -> LigneUi(e, l) })
    }.stateIn(viewModelScope, partage, ResultatsUi.VIDE)

    fun modifierClasse(nom: String, coefficient: Int) {
        val actuelle = classe.value ?: return
        viewModelScope.launch {
            val maj = actuelle.copy(nom = nom.trim(), coefficient = coefficient)
            repo.mettreAJourClasse(maj)
            classe.value = maj
        }
    }

    fun ajouterInscrit(nom: String, prenom: String, matricule: String) {
        viewModelScope.launch { repo.ajouterInscrit(classeId, nom, prenom, matricule) }
    }

    /** Ajoute une liste collée ; retourne le nombre d'élèves ajoutés. */
    fun ajouterListe(texte: String, apres: (Int) -> Unit) {
        viewModelScope.launch {
            apres(repo.ajouterInscrits(classeId, ListeElevesParser.analyser(texte)))
        }
    }

    fun modifierInscrit(inscrit: Inscrit, nom: String, prenom: String, matricule: String) {
        viewModelScope.launch {
            repo.mettreAJourInscrit(inscrit.copy(nom = nom.trim(), prenom = prenom.trim(), matricule = matricule.trim()))
        }
    }

    fun supprimerInscrit(inscrit: Inscrit) {
        viewModelScope.launch { repo.supprimerInscrit(inscrit) }
    }

    /** Retourne false (via [apres]) si la période compte déjà 2 devoirs. */
    fun ajouterEvaluation(type: TypeNote, intitule: String, apres: (Boolean) -> Unit) {
        val periode = periodeCourante.value ?: return apres(false)
        viewModelScope.launch { apres(repo.ajouterEvaluation(periode, type, intitule) != null) }
    }

    fun supprimerEvaluation(evaluation: Evaluation) {
        viewModelScope.launch { repo.supprimerEvaluation(evaluation) }
    }

    fun saisirNote(evaluationId: Long, inscritId: Long, valeur: Double?) {
        viewModelScope.launch { repo.saisirNote(evaluationId, inscritId, valeur) }
    }
}

class EnseignantViewModelFactory(
    private val repo: EnseignantRepository,
    private val classeId: Long = 0,
    private val contexte: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(EnseignantViewModel::class.java) -> EnseignantViewModel(repo) as T
        modelClass.isAssignableFrom(ClasseViewModel::class.java) -> ClasseViewModel(repo, classeId) as T
        modelClass.isAssignableFrom(EmploiViewModel::class.java) ->
            EmploiViewModel(repo, requireNotNull(contexte).applicationContext) as T
        else -> throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
    }
}
