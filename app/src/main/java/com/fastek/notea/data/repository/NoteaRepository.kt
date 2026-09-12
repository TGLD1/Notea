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

    // --- Matiere ---
    fun observerMatieres(eleveId: Long): Flow<List<Matiere>> = matiereDao.observerMatieres(eleveId)
    suspend fun getMatiere(matiereId: Long): Matiere? = matiereDao.getMatiere(matiereId)
    suspend fun ajouterMatiere(matiere: Matiere): Long = matiereDao.inserer(matiere)
    suspend fun mettreAJourMatiere(matiere: Matiere) = matiereDao.mettreAJour(matiere)
    suspend fun supprimerMatiere(matiere: Matiere) = matiereDao.supprimer(matiere)

    // --- Periode ---
    fun observerPeriodes(eleveId: Long): Flow<List<Periode>> = periodeDao.observerPeriodes(eleveId)
    suspend fun creerPeriode(periode: Periode): Long = periodeDao.inserer(periode)
    suspend fun enregistrerConduite(periodeId: Long, note: Double) =
        periodeDao.mettreAJourConduite(periodeId, note)
    suspend fun definirObjectifPeriode(periodeId: Long, objectif: Double) =
        periodeDao.mettreAJourObjectif(periodeId, objectif)

    // --- Note ---
    fun observerNotes(matiereId: Long, periodeId: Long): Flow<List<Note>> =
        noteDao.observerNotes(matiereId, periodeId)
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

    /** Moyenne générale d'une période, dérivée des moyennes par matière + la conduite. */
    fun observerMoyenneGeneralePeriode(eleveId: Long, periode: Periode): Flow<Double?> =
        observerMatieresAvecMoyenne(eleveId, periode.id, periode.objectifCible).map { liste ->
            val moyennesPonderees = liste.mapNotNull { item ->
                item.moyenne?.let { MoyenneCalculator.MoyennePonderee(it, item.matiere.coefficient) }
            }
            MoyenneCalculator.moyenneGeneralePeriode(moyennesPonderees, periode.noteConduite)
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
}
