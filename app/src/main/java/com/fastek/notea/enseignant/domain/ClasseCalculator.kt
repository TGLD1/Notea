package com.fastek.notea.enseignant.domain

import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.domain.calcul.MoyenneCalculator
import java.time.LocalDate

/** Une note saisie pour un élève : seul le type et la valeur comptent pour le calcul. */
data class NoteSaisie(val type: TypeNote, val valeur: Double)

/** Les notes d'un élève pour une période. Une évaluation sans note n'apparaît tout simplement pas. */
data class EleveNotes(val id: Long, val notes: List<NoteSaisie>)

data class LigneResultat(
    val inscritId: Long,
    val moyenneInterro: Double?,
    /** Moyenne de la période, arrondie à 2 décimales. */
    val moyenne: Double?,
    val rang: Int?
)

data class StatistiquesClasse(
    /** Nombre d'élèves ayant une moyenne. */
    val effectifNote: Int,
    val moyenneClasse: Double?,
    val plusForte: Double?,
    val plusFaible: Double?,
    /** Part des élèves à 10/20 ou plus, entre 0 et 1. */
    val tauxReussite: Double?,
    /** Effectifs par tranche : [0 ; 5[, [5 ; 10[, [10 ; 15[, [15 ; 20]. */
    val repartition: List<Int>
)

data class ResultatPeriode(val lignes: List<LigneResultat>, val stats: StatistiquesClasse)

/**
 * Calculs de classe de Notea Enseignant. Les moyennes d'élève viennent du moteur de Notea
 * ([MoyenneCalculator]) sans aucune règle supplémentaire : une seule matière, donc la moyenne
 * de la période d'un élève est sa moyenne dans la matière du professeur.
 */
object ClasseCalculator {

    private val DATE_NEUTRE: LocalDate = LocalDate.of(2000, 1, 1)

    /** Arrondi à 2 décimales, comme le bulletin et le classeur Excel. */
    fun arrondir(valeur: Double): Double = Math.round(valeur * 100.0) / 100.0

    private fun versNotes(notes: List<NoteSaisie>, type: TypeNote? = null): List<Note> =
        notes.filter { type == null || it.type == type }.map {
            Note(matiereId = 0, periodeId = 0, type = it.type, valeur = it.valeur, date = DATE_NEUTRE)
        }

    /** Moyenne des interrogations seules (null s'il n'y en a aucune). */
    fun moyenneInterro(notes: List<NoteSaisie>): Double? =
        MoyenneCalculator.moyenneMatiere(versNotes(notes, TypeNote.INTERROGATION))

    /** Moyenne de la période pour un élève (null tant qu'il n'a aucune note), arrondie à 2 décimales. */
    fun moyenneEleve(notes: List<NoteSaisie>): Double? =
        MoyenneCalculator.moyenneMatiere(versNotes(notes))?.let { arrondir(it) }

    /** Rang « à la française » : les ex æquo partagent le même rang, le suivant est décalé. */
    fun rangs(moyennes: List<Double?>): List<Int?> = moyennes.map { m ->
        if (m == null) null else 1 + moyennes.count { it != null && it > m }
    }

    fun calculerPeriode(eleves: List<EleveNotes>): ResultatPeriode {
        val moyennes = eleves.map { moyenneEleve(it.notes) }
        val rangs = rangs(moyennes)
        val lignes = eleves.mapIndexed { i, e ->
            LigneResultat(e.id, moyenneInterro(e.notes)?.let { arrondir(it) }, moyennes[i], rangs[i])
        }
        return ResultatPeriode(lignes, statistiques(moyennes))
    }

    fun statistiques(moyennes: List<Double?>): StatistiquesClasse {
        val valeurs = moyennes.filterNotNull()
        val repartition = MutableList(4) { 0 }
        valeurs.forEach { v ->
            val tranche = when {
                v < 5.0 -> 0
                v < 10.0 -> 1
                v < 15.0 -> 2
                else -> 3
            }
            repartition[tranche]++
        }
        if (valeurs.isEmpty()) return StatistiquesClasse(0, null, null, null, null, repartition)
        return StatistiquesClasse(
            effectifNote = valeurs.size,
            moyenneClasse = arrondir(valeurs.average()),
            plusForte = valeurs.max(),
            plusFaible = valeurs.min(),
            tauxReussite = valeurs.count { it >= MoyenneCalculator.SEUIL_REUSSITE }.toDouble() / valeurs.size,
            repartition = repartition
        )
    }

    /**
     * Moyenne annuelle à partir des moyennes de période (S1, S2 en public ; T1, T2, T3 en privé).
     * Null tant qu'une des périodes n'a pas de moyenne.
     */
    fun moyenneAnnuelle(type: TypeEtablissement, moyennesPeriodes: List<Double?>): Double? {
        val m = moyennesPeriodes
        return when (type) {
            TypeEtablissement.PUBLIC ->
                if (m.size >= 2 && m[0] != null && m[1] != null)
                    arrondir(MoyenneCalculator.moyenneAnnuellePublic(m[0]!!, m[1]!!)) else null
            TypeEtablissement.PRIVE ->
                if (m.size >= 3 && m[0] != null && m[1] != null && m[2] != null)
                    arrondir(MoyenneCalculator.moyenneAnnuellePrive(m[0]!!, m[1]!!, m[2]!!)) else null
        }
    }
}
