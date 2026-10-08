package com.fastek.notea.data.repository

import com.fastek.notea.data.local.dao.EleveDao
import com.fastek.notea.data.local.dao.EvenementDao
import com.fastek.notea.data.local.dao.MatiereDao
import com.fastek.notea.data.local.dao.NoteDao
import com.fastek.notea.data.local.dao.PeriodeDao
import com.fastek.notea.data.local.entity.Eleve
import com.fastek.notea.data.local.entity.Evenement
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.Periode
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.domain.bulletin.BulletinData
import com.fastek.notea.domain.bulletin.LigneBulletin
import com.fastek.notea.domain.calcul.MoyenneCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Point d'accès unique aux données. Combine les DAO Room et [MoyenneCalculator]
 * pour exposer des flux déjà prêts à afficher (moyennes recalculées automatiquement
 * dès qu'une note change).
 */
class NoteaRepository(
    private val eleveDao: EleveDao,
    private val matiereDao: MatiereDao,
    private val noteDao: NoteDao,
    private val periodeDao: PeriodeDao,
    private val evenementDao: EvenementDao
) {
    // --- Eleve ---
    fun observerProfil(): Flow<Eleve?> = eleveDao.observerProfil()
    suspend fun getProfil(): Eleve? = eleveDao.getProfil()
    suspend fun creerProfil(eleve: Eleve): Long = eleveDao.inserer(eleve)
    suspend fun mettreAJourProfil(eleve: Eleve) = eleveDao.mettreAJour(eleve)
    suspend fun mettreAJourObjectifAnnuel(eleveId: Long, objectif: Double) =
        eleveDao.mettreAJourObjectifAnnuel(eleveId, objectif)
    suspend fun mettreAJourNom(eleveId: Long, nom: String) = eleveDao.mettreAJourNom(eleveId, nom)
    suspend fun mettreAJourPrenom(eleveId: Long, prenom: String) = eleveDao.mettreAJourPrenom(eleveId, prenom)
    suspend fun mettreAJourMatricule(eleveId: Long, matricule: String) =
        eleveDao.mettreAJourMatricule(eleveId, matricule)

    // --- Matiere ---
    fun observerMatieres(eleveId: Long): Flow<List<Matiere>> = matiereDao.observerMatieres(eleveId)
    suspend fun getMatiere(matiereId: Long): Matiere? = matiereDao.getMatiere(matiereId)
    suspend fun ajouterMatiere(matiere: Matiere): Long = matiereDao.inserer(matiere)
    suspend fun mettreAJourMatiere(matiere: Matiere) = matiereDao.mettreAJour(matiere)
    suspend fun supprimerMatiere(matiere: Matiere) = matiereDao.supprimer(matiere)

    // --- Periode ---
    fun observerPeriodes(eleveId: Long): Flow<List<Periode>> = periodeDao.observerPeriodes(eleveId)
    suspend fun creerPeriode(periode: Periode): Long = periodeDao.inserer(periode)
    /**
     * Enregistre la note unique de Conduite pour une période. Conduite reste une matière
     * normale (coefficient 1), mais sa note est unique et remplace la précédente.
     */
    suspend fun enregistrerConduite(periodeId: Long, note: Double) {
        val profil = getProfil() ?: return
        val conduite = matiereDao.observerMatieres(profil.id).first().firstOrNull { it.nom == "Conduite" }
            ?: return
        noteDao.supprimerToutesPourMatierePeriode(conduite.id, periodeId)
        noteDao.inserer(
            Note(
                matiereId = conduite.id,
                periodeId = periodeId,
                type = com.fastek.notea.data.local.entity.TypeNote.INTERROGATION,
                valeur = note,
                date = LocalDate.now()
            )
        )
    }
    suspend fun definirObjectifPeriode(periodeId: Long, objectif: Double) =
        periodeDao.mettreAJourObjectif(periodeId, objectif)

    // --- Note ---
    fun observerNotes(matiereId: Long, periodeId: Long): Flow<List<Note>> =
        noteDao.observerNotes(matiereId, periodeId)

    /** Note unique de Conduite, stockée comme une note normale de la matière Conduite. */
    fun observerConduite(eleveId: Long, periodeId: Long): Flow<Double?> =
        observerMatieres(eleveId).flatMapLatest { matieres ->
            val conduite = matieres.firstOrNull { it.nom == "Conduite" }
            if (conduite == null) {
                flowOf(null)
            } else {
                observerNotes(conduite.id, periodeId).map { notes ->
                    notes.maxByOrNull { it.id }?.valeur
                }
            }
        }
    suspend fun ajouterNote(note: Note): Long = noteDao.inserer(note)
    suspend fun mettreAJourNote(note: Note) = noteDao.mettreAJour(note)
    suspend fun supprimerNote(note: Note) = noteDao.supprimer(note)

    // --- Evenement ---
    fun observerEvenements(eleveId: Long): Flow<List<Evenement>> = evenementDao.observerEvenements(eleveId)
    suspend fun ajouterEvenement(evenement: Evenement): Long = evenementDao.inserer(evenement)
    suspend fun mettreAJourEvenement(evenement: Evenement) = evenementDao.mettreAJour(evenement)
    suspend fun supprimerEvenement(evenement: Evenement) = evenementDao.supprimer(evenement)
    suspend fun supprimerEvenementParId(id: Long) = evenementDao.supprimerParId(id)
    suspend fun getEvenementsAvenirNonNotifies(eleveId: Long, debut: LocalDate, fin: LocalDate): List<Evenement> =
        evenementDao.getEvenementsAvenirNonNotifies(eleveId, debut, fin)

    // --- Données combinées (moyennes calculées en direct) ---

    data class MatiereAvecMoyenne(
        val matiere: Matiere,
        val moyenne: Double?,
        val statut: MoyenneCalculator.StatutObjectif?
    )

    /**
     * Matières de l'élève pour une période, chacune avec sa moyenne — recalculée
     * automatiquement dès qu'une note est ajoutée/modifiée/supprimée.
     */
    fun observerMatieresAvecMoyenne(
        eleveId: Long,
        periodeId: Long,
        objectifPeriode: Double
    ): Flow<List<MatiereAvecMoyenne>> =
        observerMatieres(eleveId).flatMapLatest { matieres ->
            if (matieres.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(matieres.map { matiere ->
                    observerNotes(matiere.id, periodeId).map { notes -> matiere to notes }
                }) { pairs ->
                    pairs.map { (matiere, notes) ->
                        val moyenne = MoyenneCalculator.moyenneMatiere(notes)
                        val statut = moyenne?.let { MoyenneCalculator.statutObjectif(it, objectifPeriode) }
                        MatiereAvecMoyenne(matiere, moyenne, statut)
                    }
                }
            }
        }

    /** Moyenne générale d'une période, dérivée uniquement des matières (Conduite comprise). */
    fun observerMoyenneGeneralePeriode(eleveId: Long, periode: Periode): Flow<Double?> =
        observerMatieresAvecMoyenne(eleveId, periode.id, periode.objectifCible).map { liste ->
            val moyennesPonderees = liste.mapNotNull { item ->
                item.moyenne?.let { MoyenneCalculator.MoyennePonderee(it, item.matiere.coefficient) }
            }
            MoyenneCalculator.moyenneGeneralePeriode(moyennesPonderees)
        }

    /**
     * Moyenne annuelle (établissement public). Prend un instantané des moyennes
     * de S1 et S2 au moment de l'appel — à utiliser pour le bulletin de fin d'année,
     * pas pour un affichage réactif continu.
     */
    suspend fun calculerMoyenneAnnuellePublic(eleveId: Long): Double? {
        val periodes = periodeDao.observerPeriodes(eleveId).first()
        val periode1 = periodes.find { it.numero == 1 } ?: return null
        val periode2 = periodes.find { it.numero == 2 } ?: return null

        val moyenneS1 = observerMoyenneGeneralePeriode(eleveId, periode1).first() ?: return null
        val moyenneS2 = observerMoyenneGeneralePeriode(eleveId, periode2).first() ?: return null

        return MoyenneCalculator.moyenneAnnuellePublic(moyenneS1, moyenneS2)
    }

    /**
     * Instantané des données du bulletin d'une période (pour la génération du PDF).
     * Les matières sans note sont affichées mais ne comptent pas dans la moyenne générale,
     * comme dans le reste de l'app. Retourne null si le profil ou la période n'existe pas.
     */
    suspend fun construireBulletin(numeroPeriode: Int): BulletinData? {
        val eleve = getProfil() ?: return null
        val periode = periodeDao.getPeriode(eleve.id, numeroPeriode) ?: return null
        val matieres = matiereDao.observerMatieres(eleve.id).first()

        val lignes = matieres.map { matiere ->
            val notes = noteDao.getNotes(matiere.id, periode.id)
            val interros = notes.filter { it.type == TypeNote.INTERROGATION }.map { it.valeur }
            val devoirs = notes.filter { it.type == TypeNote.DEVOIR }.map { it.valeur }
            LigneBulletin(
                matiere = matiere.nom,
                coefficient = matiere.coefficient,
                estConduite = matiere.nom == "Conduite",
                moyenneInterro = interros.takeIf { it.isNotEmpty() }?.average(),
                devoir1 = devoirs.getOrNull(0),
                devoir2 = devoirs.getOrNull(1),
                moyenne = MoyenneCalculator.moyenneMatiere(notes)
            )
        }

        val notees = lignes.filter { it.moyenne != null }
        val moyenneGenerale = MoyenneCalculator.moyenneGeneralePeriode(
            notees.map { MoyenneCalculator.MoyennePonderee(it.moyenne!!, it.coefficient) }
        )

        val estPublic = eleve.typeEtablissement == TypeEtablissement.PUBLIC
        var moyennePeriode1: Double? = null
        var moyenneAnnuelle: Double? = null
        if (estPublic && numeroPeriode == 2) {
            val periode1 = periodeDao.getPeriode(eleve.id, 1)
            val s1: Double? = if (periode1 != null) {
                observerMoyenneGeneralePeriode(eleve.id, periode1).first()
            } else {
                null
            }
            moyennePeriode1 = s1
            if (s1 != null && moyenneGenerale != null) {
                moyenneAnnuelle = MoyenneCalculator.moyenneAnnuellePublic(s1, moyenneGenerale)
            }
        }

        return BulletinData(
            nom = eleve.nom,
            prenom = eleve.prenom,
            matricule = eleve.matricule,
            classe = eleve.classe,
            anneeScolaire = eleve.anneeScolaire,
            typeEtablissement = eleve.typeEtablissement,
            numeroPeriode = numeroPeriode,
            lignes = lignes,
            moyenneGenerale = moyenneGenerale,
            totalCoefficients = notees.sumOf { it.coefficient },
            totalMoyCoef = notees.sumOf { it.moyenneCoef ?: 0.0 },
            moyennePeriode1 = moyennePeriode1,
            moyenneAnnuelle = moyenneAnnuelle,
            finAnnee = numeroPeriode == eleve.typeEtablissement.nombrePeriodes
        )
    }
}
