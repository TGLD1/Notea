package com.fastek.notea.enseignant.domain

import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse

/**
 * Résumé d'une classe pour l'écran d'accueil : la moyenne de classe de la période la plus récente
 * qui contient des notes, et le nombre d'élèves ayant une moyenne dans cette période.
 */
data class ApercuClasse(
    val effectif: Int,
    val moyenne: Double?,
    val numeroPeriode: Int?,
    val elevesNotes: Int
) {
    companion object {
        val VIDE = ApercuClasse(0, null, null, 0)
    }
}

fun calculerApercu(
    inscrits: List<Inscrit>,
    periodes: List<PeriodeClasse>,
    evaluations: List<Evaluation>,
    notes: List<NoteEleve>
): ApercuClasse {
    val parCle = notes.associate { (it.evaluationId to it.inscritId) to it.valeur }
    for (periode in periodes.sortedByDescending { it.numero }) {
        val evaluationsPeriode = evaluations.filter { it.periodeId == periode.id }
        val eleves = inscrits.map { inscrit ->
            EleveNotes(
                inscrit.id,
                evaluationsPeriode.mapNotNull { ev -> parCle[ev.id to inscrit.id]?.let { NoteSaisie(ev.type, it) } }
            )
        }
        val stats = ClasseCalculator.calculerPeriode(eleves).stats
        if (stats.effectifNote > 0) {
            return ApercuClasse(inscrits.size, stats.moyenneClasse, periode.numero, stats.effectifNote)
        }
    }
    return ApercuClasse(inscrits.size, null, null, 0)
}
