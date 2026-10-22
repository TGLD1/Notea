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
import com.fastek.notea.diagnostic.Journal
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
    suspend fun creerProfil(eleve: Eleve): Long = Journal.suivre("profil.creation") { eleveDao.inserer(eleve) }
    suspend fun mettreAJourProfil(eleve: Eleve) = eleveDao.mettreAJour(eleve)
    suspend fun mettreAJourObjectifAnnuel(eleveId: Long, objectif: Double) =
        eleveDao.mettreAJourObjectifAnnuel(eleveId, objectif)
    suspend fun mettreAJourNom(eleveId: Long, nom: String) = eleveDao.mettreAJourNom(eleveId, nom)
    suspend fun mettreAJourPrenom(eleveId: Long, prenom: String) = eleveDao.mettreAJourPrenom(eleveId, prenom)
    suspend fun mettreAJourMatricule(eleveId: Long, matricule: String) =
        eleveDao.mettreAJourMatricule(eleveId, matricule)
    suspend fun mettreAJourDateNaissance(eleveId: Long, valeur: String) =
        eleveDao.mettreAJourDateNaissance(eleveId, valeur)
    suspend fun mettreAJourLieuNaissance(eleveId: Long, valeur: String) =
        eleveDao.mettreAJourLieuNaissance(eleveId, valeur)
    suspend fun mettreAJourEffectif(eleveId: Long, valeur: String) =
        eleveDao.mettreAJourEffectif(eleveId, valeur)
    suspend fun mettreAJourAptitudeEps(eleveId: Long, valeur: String) =
        eleveDao.mettreAJourAptitudeEps(eleveId, valeur)
    suspend fun mettreAJourPhoto(eleveId: Long, chemin: String) = eleveDao.mettreAJourPhoto(eleveId, chemin)
    suspend fun mettreAJourRedoublant(eleveId: Long, valeur: String) =
        eleveDao.mettreAJourRedoublant(eleveId, valeur)

    // --- Matiere ---
    fun observerMatieres(eleveId: Long): Flow<List<Matiere>> = matiereDao.observerMatieres(eleveId)
    suspend fun getMatiere(matiereId: Long): Matiere? = matiereDao.getMatiere(matiereId)
    suspend fun ajouterMatiere(matiere: Matiere): Long = matiereDao.inserer(matiere)
    suspend fun mettreAJourMatiere(matiere: Matiere) = matiereDao.mettreAJour(matiere)
    suspend fun supprimerMatiere(matiere: Matiere) = matiereDao.supprimer(matiere)

    // --- Periode ---
    fun observerPeriodeDerniereNote(): Flow<Long?> = noteDao.observerPeriodeDerniereNote()
    fun observerPeriodes(eleveId: Long): Flow<List<Periode>> = periodeDao.observerPeriodes(eleveId)
    suspend fun creerPeriode(periode: Periode): Long = periodeDao.inserer(periode)
    suspend fun definirObjectifPeriode(periodeId: Long, objectif: Double) =
        periodeDao.mettreAJourObjectif(periodeId, objectif)

    // --- Note ---
    fun observerNotes(matiereId: Long, periodeId: Long): Flow<List<Note>> =
        noteDao.observerNotes(matiereId, periodeId)

    suspend fun ajouterNote(note: Note): Long = Journal.suivre("note.ajout") { noteDao.inserer(note) }
    suspend fun mettreAJourNote(note: Note) = noteDao.mettreAJour(note)
    suspend fun supprimerNote(note: Note) = Journal.suivre("note.suppression") { noteDao.supprimer(note) }

    // --- Evenement ---
    fun observerEvenements(eleveId: Long): Flow<List<Evenement>> = evenementDao.observerEvenements(eleveId)
    suspend fun ajouterEvenement(evenement: Evenement): Long =
        Journal.suivre("evenement.ajout") { evenementDao.inserer(evenement) }
    suspend fun getEvenement(id: Long): Evenement? = evenementDao.getEvenement(id)
    suspend fun mettreAJourEvenement(evenement: Evenement) = evenementDao.mettreAJour(evenement)
    suspend fun supprimerEvenement(evenement: Evenement) = evenementDao.supprimer(evenement)
    suspend fun supprimerEvenementParId(id: Long) = evenementDao.supprimerParId(id)
    suspend fun getEvenementsDuJour(eleveId: Long, date: LocalDate): List<Evenement> =
        evenementDao.getEvenementsDuJour(eleveId, date)
    suspend fun getEvenementsAvenirNonNotifies(eleveId: Long, debut: LocalDate, fin: LocalDate): List<Evenement> =
        evenementDao.getEvenementsAvenirNonNotifies(eleveId, debut, fin)

    /**
     * Toutes les notes au format CSV (séparateur « ; », virgule décimale : s'ouvre directement
     * dans Excel en français). Retourne null s'il n'y a pas encore de profil.
     */
    suspend fun exporterNotesCsv(): String? {
        val eleve = getProfil() ?: return null
        val periodes = periodeDao.observerPeriodes(eleve.id).first()
        val matieres = matiereDao.observerMatieres(eleve.id).first()
        val sortie = StringBuilder("\uFEFFPériode;Matière;Coefficient;Type;Note /20;Date\n")
        fun champ(texte: String) = "\"" + texte.replace("\"", "\"\"") + "\""
        periodes.forEach { periode ->
            matieres.forEach { matiere ->
                noteDao.getNotes(matiere.id, periode.id).forEach { note ->
                    sortie.append(champ("${eleve.typeEtablissement.nomPeriode} ${periode.numero}")).append(';')
                        .append(champ(matiere.nom)).append(';')
                        .append(matiere.coefficient).append(';')
                        .append(if (note.type == TypeNote.DEVOIR) "Devoir" else "Interrogation").append(';')
                        .append(String.format(java.util.Locale.FRANCE, "%.2f", note.valeur)).append(';')
                        .append(note.date).append('\n')
                }
            }
        }
        return sortie.toString()
    }

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
     * Moyenne annuelle (public : 2 semestres, privé : 3 trimestres). Prend un instantané des
     * moyennes de toutes les périodes ; null tant qu'une période n'a aucune note.
     */
    suspend fun calculerMoyenneAnnuelle(eleveId: Long): Double? {
        val eleve = getProfil() ?: return null
        val moyennes = (1..eleve.typeEtablissement.nombrePeriodes).map { numero ->
            val periode = periodeDao.getPeriode(eleveId, numero) ?: return null
            observerMoyenneGeneralePeriode(eleveId, periode).first() ?: return null
        }
        return when (eleve.typeEtablissement) {
            TypeEtablissement.PUBLIC -> MoyenneCalculator.moyenneAnnuellePublic(moyennes[0], moyennes[1])
            TypeEtablissement.PRIVE -> MoyenneCalculator.moyenneAnnuellePrive(moyennes[0], moyennes[1], moyennes[2])
        }
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

        val finAnnee = numeroPeriode == eleve.typeEtablissement.nombrePeriodes
        var moyennesPrecedentes: List<Double?> = emptyList()
        var moyenneAnnuelle: Double? = null
        if (finAnnee && numeroPeriode > 1) {
            moyennesPrecedentes = (1 until numeroPeriode).map { numero ->
                periodeDao.getPeriode(eleve.id, numero)?.let { observerMoyenneGeneralePeriode(eleve.id, it).first() }
            }
            val toutes = moyennesPrecedentes + moyenneGenerale
            if (toutes.all { it != null }) {
                moyenneAnnuelle = when (eleve.typeEtablissement) {
                    TypeEtablissement.PUBLIC ->
                        MoyenneCalculator.moyenneAnnuellePublic(toutes[0]!!, toutes[1]!!)
                    TypeEtablissement.PRIVE ->
                        MoyenneCalculator.moyenneAnnuellePrive(toutes[0]!!, toutes[1]!!, toutes[2]!!)
                }
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
            moyennesPrecedentes = moyennesPrecedentes,
            moyenneAnnuelle = moyenneAnnuelle,
            finAnnee = finAnnee,
            dateNaissance = eleve.dateNaissance,
            lieuNaissance = eleve.lieuNaissance,
            effectif = eleve.effectif,
            aptitudeEps = eleve.aptitudeEps,
            redoublant = eleve.redoublant,
            photo = eleve.photo
        )
    }
}
