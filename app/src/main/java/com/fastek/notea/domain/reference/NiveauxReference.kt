package com.fastek.notea.domain.reference

/** Niveaux et séries du secondaire béninois, pour un choix en liste plutôt qu'en texte libre. */
object NiveauxReference {
    val NIVEAUX = listOf("6e", "5e", "4e", "3e", "2nde", "1ère", "Tle")
    val SERIES = listOf("A", "B", "C", "D")

    private val NIVEAUX_AVEC_SERIE = setOf("2nde", "1ère", "Tle")

    /** Le collège (6e-3e) n'a pas de série ; le lycée (2nde-Tle) en a une. */
    fun necessiteSerie(niveau: String): Boolean = niveau in NIVEAUX_AVEC_SERIE
}
