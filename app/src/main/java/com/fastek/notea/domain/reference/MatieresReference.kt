package com.fastek.notea.domain.reference

/**
 * Liste de matières proposées à l'onboarding — toutes disponibles d'un coup,
 * l'élève coche celles qui le concernent et saisit lui-même le coefficient
 * (celui-ci varie selon la classe/série, pas de table officielle à maintenir).
 */
object MatieresReference {
    val LISTE = listOf(
        "Français",
        "Anglais",
        "Mathématiques",
        "PCT",
        "SVT",
        "Histoire-Géographie",
        "EPS",
        "Éducation Civique et Morale",
        "Philosophie",
        "Espagnol",
        "Allemand",
        "Arts Plastiques",
        "Informatique",
        "Économie",
        "Comptabilité",
        "Droit"
    )
}
