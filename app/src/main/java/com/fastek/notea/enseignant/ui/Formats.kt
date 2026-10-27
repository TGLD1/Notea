package com.fastek.notea.enseignant.ui

import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import java.util.Locale
import kotlin.math.roundToInt

/** Note saisie : « 12 » ou « 12,5 ». */
fun formaterNote(valeur: Double): String =
    if (valeur == Math.floor(valeur)) valeur.toInt().toString() else valeur.toString().replace('.', ',')

/** Moyenne affichée avec 2 décimales : « 10,33 ». */
fun formaterMoyenne(valeur: Double?): String =
    if (valeur == null) "—" else String.format(Locale.FRANCE, "%.2f", valeur)

fun formaterTaux(taux: Double?): String =
    if (taux == null) "—" else "${(taux * 100).roundToInt()} %"

fun nomComplet(inscrit: Inscrit): String = "${inscrit.nom} ${inscrit.prenom}".trim()

/** Intitulé saisi, sinon « Interrogation 2 » ou « Devoir 1 » selon le rang dans la période. */
fun libelleEvaluation(evaluation: Evaluation, toutes: List<Evaluation>): String {
    if (evaluation.intitule.isNotBlank()) return evaluation.intitule
    val rang = toutes.filter { it.type == evaluation.type }.indexOfFirst { it.id == evaluation.id } + 1
    return if (evaluation.type == TypeNote.DEVOIR) "Devoir $rang" else "Interrogation $rang"
}
