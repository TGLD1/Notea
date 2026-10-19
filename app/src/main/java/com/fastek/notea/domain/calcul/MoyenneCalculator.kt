package com.fastek.notea.domain.calcul

import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import java.time.LocalDate

/**
 * Moteur de calcul des moyennes — système scolaire béninois : PUBLIC (2 semestres) et PRIVÉ (3 trimestres).
 *
 * Règle par matière et par période :
 *   1. M_intero = moyenne simple de toutes les notes d'interrogation de la matière.
 *   2. M_intero compte comme UNE seule valeur, à égalité avec chaque note de devoir prise
 *      individuellement.
 *   3. Moyenne matière = (M_intero + somme des notes de devoir) / (1 + nombre de devoirs)
 *
 * Exemple validé (Matière A, coeff 2) : interros 4,3,3,1 -> M_intero = 2.75
 *   devoirs 3,4 -> (2.75 + 3 + 4) / 3 = 3.25
 *
 * Moyenne générale d'une période :
 *   Σ(moyenne matière × coefficient) / Σ coefficients.
 *   Conduite est déjà incluse comme matière coefficient 1 lorsqu'elle est notée.
 *
 * Moyenne annuelle (public) :
 *   (2 × moyenne S2 + moyenne S1) / 3   — le 2e semestre pèse double (poids 2 + poids 1 = 3).
 *
 * Moyenne annuelle (privé) :
 *   (T1 + T2 + T3) / 3 — les trois trimestres pèsent autant.
 */
object MoyenneCalculator {

    /** Seuil de réussite officiel. */
    const val SEUIL_REUSSITE = 10.0

    /** Objectif par défaut proposé par l'app (au-dessus du seuil officiel). */
    const val OBJECTIF_PAR_DEFAUT = 12.0

    /**
     * Moyenne d'une matière pour une période, à partir de ses notes.
     * Retourne null tant qu'aucune note n'a été saisie.
     */
    fun moyenneMatiere(notes: List<Note>): Double? {
        val interros = notes.filter { it.type == TypeNote.INTERROGATION }.map { it.valeur }
        val devoirs = notes.filter { it.type == TypeNote.DEVOIR }.map { it.valeur }

        if (interros.isEmpty() && devoirs.isEmpty()) return null

        val mIntero = interros.takeIf { it.isNotEmpty() }?.average()

        return when {
            mIntero != null && devoirs.isNotEmpty() ->
                (mIntero + devoirs.sum()) / (1 + devoirs.size)
            mIntero != null -> mIntero
            else -> devoirs.average()
        }
    }

    /** Moyenne d'une matière avec son coefficient, prête pour la moyenne générale pondérée. */
    data class MoyennePonderee(val moyenne: Double, val coefficient: Int)

    /**
     * Moyenne générale d'une période.
     * Conduite est une matière ordinaire et doit donc être présente dans la liste avec
     * son coefficient 1 lorsqu'une note lui a été attribuée.
     */
    fun moyenneGeneralePeriode(
        moyennesMatieres: List<MoyennePonderee>
    ): Double? {
        if (moyennesMatieres.isEmpty()) return null

        val sommePonderee = moyennesMatieres.sumOf { it.moyenne * it.coefficient }
        val sommeCoefficients = moyennesMatieres.sumOf { it.coefficient }

        return if (sommeCoefficients > 0) sommePonderee / sommeCoefficients else null
    }

    /**
     * Moyenne annuelle pour un établissement PUBLIC (2 semestres).
     * Le 2e semestre compte double : (2×S2 + 1×S1) / 3.
     */
    fun moyenneAnnuellePublic(moyenneS1: Double, moyenneS2: Double): Double =
        (2 * moyenneS2 + moyenneS1) / 3

    /**
     * Moyenne annuelle pour un établissement PRIVÉ (3 trimestres) :
     * (T1 + T2 + T3) / 3 — les trimestres ont tous le même poids.
     */
    fun moyenneAnnuellePrive(moyenneT1: Double, moyenneT2: Double, moyenneT3: Double): Double =
        (moyenneT1 + moyenneT2 + moyenneT3) / 3

    /** Issue d'un calcul « il me faut X » : atteignable, déjà acquis, ou hors de portée. */
    enum class StatutRequis { ATTEIGNABLE, DEJA_ATTEINT, IMPOSSIBLE }

    /**
     * Résultat d'un calcul inverse. [valeur] est la note (ou moyenne) requise, arrondie au
     * centième supérieur ; [maximum] n'est renseigné que pour [StatutRequis.IMPOSSIBLE] d'une
     * note à saisir : la meilleure moyenne générale obtenable avec 20/20.
     */
    data class Requis(val statut: StatutRequis, val valeur: Double, val maximum: Double? = null)

    private fun arrondiSuperieur(valeur: Double): Double = Math.ceil(valeur * 100.0 - 1e-9) / 100.0

    /**
     * Note minimale à obtenir au prochain devoir (ou à la prochaine interrogation) d'une matière
     * pour que la moyenne générale de la période atteigne [cible].
     * [autres] : moyennes pondérées des autres matières déjà notées.
     */
    fun noteRequisePourMoyenneGenerale(
        notesMatiere: List<Note>,
        type: TypeNote,
        coefficient: Int,
        autres: List<MoyennePonderee>,
        cible: Double
    ): Requis {
        fun moyenneGenerale(x: Double): Double {
            val hypothese = Note(
                matiereId = 0, periodeId = 0, type = type, valeur = x, date = LocalDate.now()
            )
            val moyenneMatiere = moyenneMatiere(notesMatiere + hypothese) ?: x
            val somme = autres.sumOf { it.moyenne * it.coefficient } + moyenneMatiere * coefficient
            val poids = autres.sumOf { it.coefficient } + coefficient
            return somme / poids
        }

        val avecZero = moyenneGenerale(0.0)
        val avecVingt = moyenneGenerale(20.0)
        return when {
            cible <= avecZero -> Requis(StatutRequis.DEJA_ATTEINT, 0.0)
            cible > avecVingt + 1e-9 -> Requis(StatutRequis.IMPOSSIBLE, 20.0, avecVingt)
            else -> Requis(
                StatutRequis.ATTEIGNABLE,
                arrondiSuperieur(20.0 * (cible - avecZero) / (avecVingt - avecZero))
            )
        }
    }

    /**
     * Moyenne à obtenir à la dernière période (2e semestre ou 3e trimestre) pour que la moyenne
     * annuelle atteigne [cible], connaissant les moyennes des périodes précédentes.
     *  - public : (2 × S2 + S1) / 3 = cible
     *  - privé  : (T1 + T2 + T3) / 3 = cible
     */
    fun moyenneRequiseDernierePeriode(
        type: TypeEtablissement,
        precedentes: List<Double>,
        cible: Double
    ): Requis {
        val requise = when (type) {
            TypeEtablissement.PUBLIC -> (3 * cible - precedentes[0]) / 2
            TypeEtablissement.PRIVE -> 3 * cible - precedentes[0] - precedentes[1]
        }
        return when {
            requise <= 0.0 -> Requis(StatutRequis.DEJA_ATTEINT, 0.0)
            requise > 20.0 + 1e-9 -> Requis(StatutRequis.IMPOSSIBLE, arrondiSuperieur(requise))
            else -> Requis(StatutRequis.ATTEIGNABLE, arrondiSuperieur(requise))
        }
    }

    /** Statut visuel (pour l'effet "glow" du dashboard) selon l'écart à l'objectif. */
    enum class StatutObjectif { ATTEINT, PROCHE, LOIN }

    fun statutObjectif(moyenne: Double, objectif: Double): StatutObjectif = when {
        moyenne >= objectif -> StatutObjectif.ATTEINT
        moyenne >= objectif - 2 -> StatutObjectif.PROCHE
        else -> StatutObjectif.LOIN
    }

    /**
     * Reconstruit la moyenne générale telle qu'elle aurait été à une date donnée, en ne
     * comptant que les notes déjà saisies à cette date (pas de stockage d'historique séparé
     * nécessaire — chaque note a déjà sa propre date).
     */
    fun moyenneGeneraleADate(
        matieresAvecNotes: List<Pair<Matiere, List<Note>>>,
        date: LocalDate
    ): Double? {
        val moyennesPonderees = matieresAvecNotes.mapNotNull { (matiere, notes) ->
            val notesJusquaDate = notes.filter { !it.date.isAfter(date) }
            moyenneMatiere(notesJusquaDate)?.let { MoyennePonderee(it, matiere.coefficient) }
        }
        return moyenneGeneralePeriode(moyennesPonderees)
    }
}
