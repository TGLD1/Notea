package com.fastek.notea.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Profil de l'élève, créé lors de l'onboarding (premier lancement).
 * Une seule ligne existe normalement dans cette table (app mono-utilisateur, 100% locale).
 *
 * @param objectifAnnuel objectif de moyenne annuelle sur 20 (par défaut 12/20, le seuil
 *   officiel de passage étant 10/20).
 */
@Entity(tableName = "eleve")
data class Eleve(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val prenom: String,
    val classe: String,
    val typeEtablissement: TypeEtablissement,
    val anneeScolaire: String,
    val objectifAnnuel: Double = 12.0
)
