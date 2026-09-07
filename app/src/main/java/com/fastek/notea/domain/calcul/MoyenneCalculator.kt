package com.fastek.notea.domain.calcul

import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.TypeNote

/**
 * Moteur de calcul des moyennes — système scolaire béninois, établissement PUBLIC (2 semestres).
 * La branche PRIVE (3 trimestres) suit le même principe mais n'est pas encore câblée.
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
 *   Σ(moyenne matière × coefficient) / Σ coefficients, + note de conduite (coefficient 1)
 *   dès qu'elle est disponible (attribuée en fin de période par le conseil des professeurs).
 *
 * Moyenne annuelle (public) :
 *   (2 × moyenne S2 + moyenne S1) / 3   — le 2e semestre pèse double (poids 2 + poids 1 = 3).
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
     * Moyenne générale d'une période (semestre).
     *
     * @param moyennesMatieres moyenne + coefficient de chaque matière ayant au moins une note
     * @param noteConduite note de conduite /20 (coefficient 1) ; null si pas encore attribuée
     *   par le conseil des professeurs — dans ce cas elle est simplement absente du calcul.
     */
    fun moyenneGeneralePeriode(
        moyennesMatieres: List<MoyennePonderee>,
        noteConduite: Double? = null
    ): Double? {
        if (moyennesMatieres.isEmpty()) return null

        var sommePonderee = moyennesMatieres.sumOf { it.moyenne * it.coefficient }
        var sommeCoefficients = moyennesMatieres.sumOf { it.coefficient }

        if (noteConduite != null) {
            sommePonderee += noteConduite
            sommeCoefficients += 1
        }

        return if (sommeCoefficients > 0) sommePonderee / sommeCoefficients else null
    }

    /**
     * Moyenne annuelle pour un établissement PUBLIC (2 semestres).
     * Le 2e semestre compte double : (2×S2 + 1×S1) / 3.
     */
    fun moyenneAnnuellePublic(moyenneS1: Double, moyenneS2: Double): Double =
        (2 * moyenneS2 + moyenneS1) / 3

    /** Statut visuel (pour l'effet "glow" du dashboard) selon l'écart à l'objectif. */
    enum class StatutObjectif { ATTEINT, PROCHE, LOIN }

    fun statutObjectif(moyenne: Double, objectif: Double): StatutObjectif = when {
        moyenne >= objectif -> StatutObjectif.ATTEINT
        moyenne >= objectif - 2 -> StatutObjectif.PROCHE
        else -> StatutObjectif.LOIN
    }
}
