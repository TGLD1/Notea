package com.fastek.notea.data.repository

import com.fastek.notea.data.local.entity.Periode

/**
 * Période affichée : celle que l'élève a choisie ; sinon la première période qui n'est pas marquée
 * comme terminée (Semestre 1 tant qu'il n'est pas terminé) ; à défaut, la dernière.
 */
object PeriodeCourante {
    fun choisir(periodes: List<Periode>, choix: Int?): Periode? =
        periodes.find { it.numero == choix }
            ?: periodes.sortedBy { it.numero }.firstOrNull { !it.terminee }
            ?: periodes.maxByOrNull { it.numero }
}
