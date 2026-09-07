package com.fastek.notea.data.local.entity

/**
 * Type d'établissement fréquenté par l'élève.
 *
 * Détermine le découpage de l'année scolaire :
 *  - PUBLIC : 2 semestres
 *  - PRIVE  : 3 trimestres
 *
 * (La branche PRIVE n'est pas encore câblée dans le moteur de calcul —
 * priorité donnée au PUBLIC dans un premier temps, même logique à terme.)
 */
enum class TypeEtablissement {
    PUBLIC,
    PRIVE;

    /** Nombre de périodes dans l'année scolaire pour ce type d'établissement. */
    val nombrePeriodes: Int
        get() = when (this) {
            PUBLIC -> 2
            PRIVE -> 3
        }
}
