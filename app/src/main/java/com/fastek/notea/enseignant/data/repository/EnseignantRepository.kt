package com.fastek.notea.enseignant.data.repository

import androidx.room.withTransaction
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.data.EnseignantDatabase
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import com.fastek.notea.enseignant.data.entity.Professeur
import com.fastek.notea.enseignant.domain.EleveLu
import com.fastek.notea.enseignant.export.ClasseExport
import com.fastek.notea.enseignant.export.ClasseurExport
import com.fastek.notea.enseignant.export.EleveExport
import com.fastek.notea.enseignant.export.EvaluationExport
import com.fastek.notea.enseignant.export.PeriodeExport
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Accès aux données de Notea Enseignant : profil, classes, élèves, évaluations et notes. */
class EnseignantRepository(private val db: EnseignantDatabase) {

    companion object {
        /** Règle de Notea : au plus 2 devoirs par période. */
        const val MAX_DEVOIRS_PAR_PERIODE = 2
        const val NOTE_MAX = 20.0
    }

    private val professeurDao = db.professeurDao()
    private val classeDao = db.classeDao()
    private val inscritDao = db.inscritDao()
    private val periodeDao = db.periodeClasseDao()
    private val evaluationDao = db.evaluationDao()
    private val noteDao = db.noteEleveDao()

    // --- Profil -----------------------------------------------------------------------------

    fun observerProfesseur(): Flow<Professeur?> = professeurDao.observer()

    suspend fun creerProfesseur(professeur: Professeur): Long = professeurDao.inserer(professeur)

    suspend fun mettreAJourProfesseur(professeur: Professeur) = professeurDao.mettreAJour(professeur)

    // --- Classes ----------------------------------------------------------------------------

    fun observerClasses(professeurId: Long): Flow<List<Classe>> = classeDao.observerClasses(professeurId)

    /** Crée la classe avec toutes ses périodes (2 semestres ou 3 trimestres). */
    suspend fun creerClasse(
        professeur: Professeur,
        nom: String,
        coefficient: Int = 1
    ): Long = db.withTransaction {
        val ordre = classeDao.obtenirClasses(professeur.id).size
        val classeId = classeDao.inserer(
            Classe(professeurId = professeur.id, nom = nom.trim(), coefficient = coefficient, ordre = ordre)
        )
        for (numero in 1..professeur.typeEtablissement.nombrePeriodes) {
            periodeDao.inserer(PeriodeClasse(classeId = classeId, numero = numero))
        }
        classeId
    }

    /** Crée une classe pour le professeur enregistré : le profil est relu en base au moment de l'ajout. */
    suspend fun creerClasseDuProfesseur(nom: String, coefficient: Int = 1): Long = db.withTransaction {
        val professeur = professeurDao.obtenir()
            ?: throw IllegalStateException("Profil introuvable : crée d'abord ton profil.")
        creerClasse(professeur, nom, coefficient)
    }

    suspend fun obtenirClasse(id: Long): Classe? = classeDao.obtenir(id)

    suspend fun mettreAJourClasse(classe: Classe) = classeDao.mettreAJour(classe)

    suspend fun supprimerClasse(classe: Classe) = classeDao.supprimer(classe)

    fun observerPeriodes(classeId: Long): Flow<List<PeriodeClasse>> = periodeDao.observerPeriodes(classeId)

    suspend fun terminerPeriode(periode: PeriodeClasse, terminee: Boolean) =
        periodeDao.mettreAJour(periode.copy(terminee = terminee))

    // --- Élèves -----------------------------------------------------------------------------

    fun observerInscrits(classeId: Long): Flow<List<Inscrit>> = inscritDao.observerInscrits(classeId)

    fun observerNombreEleves(): Flow<Int> = inscritDao.observerNombre()

    suspend fun ajouterInscrit(classeId: Long, nom: String, prenom: String, matricule: String = ""): Long =
        inscritDao.inserer(Inscrit(classeId = classeId, nom = nom.trim(), prenom = prenom.trim(), matricule = matricule.trim()))

    /** Ajoute une liste d'élèves lue par `ListeElevesParser` ; retourne le nombre d'élèves ajoutés. */
    suspend fun ajouterInscrits(classeId: Long, eleves: List<EleveLu>): Int {
        val inscrits = eleves
            .filter { it.nom.isNotBlank() }
            .map { Inscrit(classeId = classeId, nom = it.nom.trim(), prenom = it.prenom.trim(), matricule = it.matricule.trim()) }
        inscritDao.insererTous(inscrits)
        return inscrits.size
    }

    suspend fun mettreAJourInscrit(inscrit: Inscrit) = inscritDao.mettreAJour(inscrit)

    suspend fun supprimerInscrit(inscrit: Inscrit) = inscritDao.supprimer(inscrit)

    // --- Évaluations et notes -----------------------------------------------------------------

    fun observerEvaluations(periodeId: Long): Flow<List<Evaluation>> = evaluationDao.observerEvaluations(periodeId)

    fun observerNotes(periodeId: Long): Flow<List<NoteEleve>> = noteDao.observerNotesPeriode(periodeId)

    fun observerEvaluationsClasse(classeId: Long): Flow<List<Evaluation>> = evaluationDao.observerEvaluationsClasse(classeId)

    fun observerNotesClasse(classeId: Long): Flow<List<NoteEleve>> = noteDao.observerNotesClasse(classeId)

    /**
     * Ajoute une évaluation à une période. Retourne son identifiant, ou null si la période
     * compte déjà 2 devoirs.
     */
    suspend fun ajouterEvaluation(
        periode: PeriodeClasse,
        type: TypeNote,
        intitule: String,
        date: LocalDate = LocalDate.now()
    ): Long? = db.withTransaction {
        if (type == TypeNote.DEVOIR &&
            evaluationDao.compterParType(periode.id, TypeNote.DEVOIR) >= MAX_DEVOIRS_PAR_PERIODE
        ) {
            null
        } else {
            evaluationDao.inserer(
                Evaluation(classeId = periode.classeId, periodeId = periode.id, type = type, intitule = intitule.trim(), date = date)
            )
        }
    }

    suspend fun mettreAJourEvaluation(evaluation: Evaluation) = evaluationDao.mettreAJour(evaluation)

    suspend fun supprimerEvaluation(evaluation: Evaluation) = evaluationDao.supprimer(evaluation)

    /**
     * Enregistre la note d'un élève à une évaluation. Une valeur nulle vide la case (élève absent :
     * ni pénalisé ni compté). Retourne false si la valeur n'est pas comprise entre 0 et 20.
     */
    suspend fun saisirNote(evaluationId: Long, inscritId: Long, valeur: Double?): Boolean {
        if (valeur == null) {
            noteDao.effacer(evaluationId, inscritId)
            return true
        }
        if (valeur.isNaN() || valeur < 0.0 || valeur > NOTE_MAX) return false
        noteDao.enregistrer(NoteEleve(evaluationId = evaluationId, inscritId = inscritId, valeur = valeur))
        return true
    }

    // --- Export -----------------------------------------------------------------------------

    /** Rassemble les données des classes demandées (toutes si [classeIds] est nul) pour le classeur Excel. */
    suspend fun preparerExport(professeur: Professeur, classeIds: Set<Long>? = null): ClasseurExport {
        val classes = classeDao.obtenirClasses(professeur.id)
            .filter { classeIds == null || it.id in classeIds }
            .map { classe ->
                val periodes = periodeDao.obtenirPeriodes(classe.id)
                val evaluations = evaluationDao.obtenirEvaluationsClasse(classe.id)
                val notes = noteDao.obtenirNotesClasse(classe.id)
                    .groupBy { it.evaluationId }
                    .mapValues { (_, liste) -> liste.associate { it.inscritId to it.valeur } }
                ClasseExport(
                    nom = classe.nom,
                    coefficient = classe.coefficient,
                    eleves = inscritDao.obtenirInscrits(classe.id)
                        .map { EleveExport(it.id, it.nom, it.prenom, it.matricule) },
                    periodes = periodes.map { p ->
                        PeriodeExport(
                            numero = p.numero,
                            evaluations = evaluations.filter { it.periodeId == p.id }
                                .map { EvaluationExport(it.id, it.type, it.intitule) }
                        )
                    },
                    notes = notes
                )
            }
        return ClasseurExport(
            professeur = "${professeur.prenom} ${professeur.nom}".trim(),
            matiere = professeur.matiere,
            type = professeur.typeEtablissement,
            anneeScolaire = professeur.anneeScolaire,
            etablissement = professeur.etablissement,
            classes = classes
        )
    }
}
