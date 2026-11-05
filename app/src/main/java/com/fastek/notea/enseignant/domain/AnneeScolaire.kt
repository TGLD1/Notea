package com.fastek.notea.enseignant.domain

object AnneeScolaire {
    /** « 2025-2026 » donne « 2026-2027 » (le séparateur est conservé) ; inchangé si le format est inconnu. */
    fun suivante(annee: String): String {
        val modele = Regex("(\\d{4})(\\D+)(\\d{4})").find(annee) ?: return annee
        val (debut, separateur, fin) = modele.destructured
        return annee.replaceRange(modele.range, "${debut.toInt() + 1}$separateur${fin.toInt() + 1}")
    }
}
