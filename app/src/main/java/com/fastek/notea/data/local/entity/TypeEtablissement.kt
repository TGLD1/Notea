package com.fastek.notea.data.local.entity

/**
 * Type d'établissement fréquenté par l'élève.
 *
 * Détermine le découpage de l'année scolaire :
 *  - PUBLIC : 2 semestres
 *  - PRIVE  : 3 trimestres
 *
 * PUBLIC : 2 semestres.
 * PRIVE : 3 trimestres.
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
